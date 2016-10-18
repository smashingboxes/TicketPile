package ticketpile.service.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.sql.deleteWhere
import ticketpile.service.database.*
import ticketpile.service.model.transformation.Weighable
import ticketpile.service.util.BigZero
import ticketpile.service.util.PrimaryEntity
import ticketpile.service.util.RelationalEntity
import ticketpile.service.util.RelationalEntityClass
import java.math.BigDecimal

/**
 * Normalized fusion of ResCalcItem and TicketCode from Advance.
 * 
 * Created by jonlatane on 8/28/16.
 */
class Ticket(id: EntityID<Int>) : PrimaryEntity(id, Tickets), Weighable {

    companion object : RelationalEntityClass<Ticket>(Tickets)
    var bookingItem by BookingItem referencedOn Tickets.parent
    
    @get:JsonProperty
    val ticketId by PK
    @get:JsonProperty
    var code by Tickets.code
    @get:JsonProperty
    var personCategory by PersonCategory referencedOn Tickets.personCategory
    @get:JsonProperty
    override var basePrice by Tickets.basePrice
    
    // Fields to references mapped adjustments
    val addOns by TicketBookingAddOn childrenOn TicketBookingAddOns.parent
    val manualAdjustments by TicketBookingManualAdjustment childrenOn TicketBookingManualAdjustments.parent
    val fees by TicketBookingFee childrenOn TicketBookingFees.parent
    val discounts by TicketBookingDiscount childrenOn TicketBookingDiscounts.parent
    val itemAddOns by TicketBookingItemAddOn childrenOn TicketBookingItemAddOns.parent

    // Adjustment value caches
    @get:JsonProperty
    override var discountsAmount by cacheNotifierColumn(
            column = Tickets.discountsAmount,
            calculation = { adjustmentTotal(discounts) },
            notifier = {
                this.grossAmount = null
                this.bookingItem.discountsAmount = null
            })
    @get:JsonProperty
    override var feesAmount by cacheNotifierColumn(
            column = Tickets.feesAmount,
            calculation = { adjustmentTotal(fees) },
            notifier = {
                this.grossAmount = null
                this.bookingItem.feesAmount = null
            })
    @get:JsonProperty
    override var addOnsAmount by cacheNotifierColumn(
            column = Tickets.addOnsAmount,
            calculation = { adjustmentTotal(addOns) },
            notifier = {
                this.grossAmount = null
                this.bookingItem.addOnsAmount = null
            })
    @get:JsonProperty
    override var manualAdjustmentsAmount by cacheNotifierColumn(
            column = Tickets.manualAdjustmentsAmount,
            calculation = { adjustmentTotal(manualAdjustments) },
            notifier = {
                this.grossAmount = null
                this.bookingItem.manualAdjustmentsAmount = null
            })
    @get:JsonProperty
    override var itemAddOnsAmount by cacheNotifierColumn(
            column = Tickets.itemAddOnsAmount,
            calculation = { adjustmentTotal(itemAddOns) },
            notifier = {
                this.grossAmount = null
                this.bookingItem.itemAddOnsAmount = null
            })
    @get:JsonProperty
    override var grossAmount : BigDecimal? by cacheNotifierColumn(
            column = Tickets.grossAmount,
            calculation = {
                basePrice + discountsAmount!! + feesAmount!! + addOnsAmount!! +
                        manualAdjustmentsAmount!! + itemAddOnsAmount!!
            },
            notifier = {
                this.bookingItem.grossAmount = null
                this.totalAmount = null
            })
    @get:JsonProperty
    override var totalAmount: BigDecimal? by cacheNotifierColumn(
            column = Tickets.totalAmount,
            calculation = {
                BigZero.max(grossAmount!!)
            },
            notifier = {
                this.bookingItem.totalAmount = null
            })
    
    // Adjustment count caches
    @get:JsonProperty
    var discountCount by cacheColumn(
            column = Tickets.discountCount,
            calculation = {
                discounts.count()
            }
    )
    @get:JsonProperty
    var feeCount by cacheColumn(
            column = Tickets.feeCount,
            calculation = {
                fees.count()
            }
    )
    @get:JsonProperty
    var addOnCount by cacheColumn(
            column = Tickets.addOnCount,
            calculation = {
                addOns.count()
            }
    )
    @get:JsonProperty
    var manualAdjustmentCount by cacheColumn(
            column = Tickets.manualAdjustmentCount,
            calculation = {
                manualAdjustments.count()
            }
    )
    @get:JsonProperty
    var itemAddOnCount by cacheColumn(
            column = Tickets.itemAddOnCount,
            calculation = {
                itemAddOns.count()
            }
    )

    override val tickets: List<Ticket>
        get() {
            return listOf(this)
        }

    fun populateCaches() {
        totalAmount!!
        
        discountCount!!
        feeCount!!
        addOnCount!!
        manualAdjustmentCount!!
        itemAddOnCount!!
    }
    
    override fun delete() {
        TicketBookingAddOns.deleteWhere {
            TicketBookingAddOns.parent eq id
        }
        TicketBookingDiscounts.deleteWhere {
            TicketBookingDiscounts.parent eq id
        }
        TicketBookingManualAdjustments.deleteWhere {
            TicketBookingManualAdjustments.parent eq id
        }
        TicketBookingItemAddOns.deleteWhere {
            TicketBookingItemAddOns.parent eq id
        }
        TicketBookingFees.deleteWhere {
            TicketBookingFees.parent eq id
        }
        super.delete()
    }
}

class TicketBookingAddOn(id: EntityID<Int>) : RelationalEntity(id), AddOnAdjustment<Ticket>, MappedAdjustment<BookingAddOn>{
    companion object : RelationalEntityClass<TicketBookingAddOn>(TicketBookingAddOns)
    override var subject by Ticket referencedOn TicketBookingAddOns.parent
    
    @get:JsonProperty
    override var addOn by AddOn referencedOn TicketBookingAddOns.addon
    @get:JsonProperty
    override var amount : BigDecimal by notifierColumn(
            column = TicketBookingAddOns.amount,
            notifier = {
                this.subject.addOnsAmount = null
                this.subject.addOnCount = null
            })
    @get:JsonProperty
    override var selection by TicketBookingAddOns.selection
    //@get:JsonProperty
    override var sourceAdjustment by BookingAddOn referencedOn TicketBookingAddOns.bookingAddOn
}

class TicketBookingDiscount(id: EntityID<Int>) : RelationalEntity(id), DiscountAdjustment<Ticket>, MappedAdjustment<BookingDiscount> {
    companion object : RelationalEntityClass<TicketBookingDiscount>(TicketBookingDiscounts)
    override var subject by Ticket referencedOn TicketBookingDiscounts.parent

    @get:JsonProperty
    override var discount by Discount referencedOn TicketBookingDiscounts.discount
    @get:JsonProperty
    override var amount : BigDecimal by notifierColumn(
            column = TicketBookingDiscounts.amount,
            notifier = {
                this.subject.discountsAmount = null
                this.subject.discountCount = null
            })
    //@get:JsonProperty
    override var sourceAdjustment by BookingDiscount referencedOn TicketBookingDiscounts.bookingDiscount
}

class TicketBookingManualAdjustment(id: EntityID<Int>) : RelationalEntity(id), ManualAdjustment<Ticket>, MappedAdjustment<BookingManualAdjustment> {
    companion object : RelationalEntityClass<TicketBookingManualAdjustment>(TicketBookingManualAdjustments)
    override var subject by Ticket referencedOn TicketBookingManualAdjustments.parent

    @get:JsonProperty
    override var description by TicketBookingManualAdjustments.description
    @get:JsonProperty
    override var amount : BigDecimal by notifierColumn(
            column = TicketBookingManualAdjustments.amount,
            notifier = {
                this.subject.manualAdjustmentsAmount = null
                this.subject.manualAdjustmentCount = null
            })
    //@get:JsonProperty
    override var sourceAdjustment by BookingManualAdjustment referencedOn TicketBookingManualAdjustments.bookingManualAdjustment
}

class TicketBookingFee(id: EntityID<Int>) : RelationalEntity(id), FeeAdjustment<Ticket>, MappedAdjustment<BookingFee> {
    companion object : RelationalEntityClass<TicketBookingFee>(TicketBookingFees)
    override var subject by Ticket referencedOn TicketBookingFees.parent

    @get:JsonProperty
    override var description by TicketBookingFees.description
    @get:JsonProperty
    override var amount : BigDecimal by notifierColumn(
            column = TicketBookingFees.amount,
            notifier = {
                this.subject.feesAmount = null
                this.subject.feeCount = null
            })
    //@get:JsonProperty
    override var sourceAdjustment by BookingFee referencedOn TicketBookingFees.bookingFee
}

class TicketBookingItemAddOn(id: EntityID<Int>) : RelationalEntity(id), AddOnAdjustment<Ticket>, MappedAdjustment<BookingItemAddOn> {
    companion object : RelationalEntityClass<TicketBookingItemAddOn>(TicketBookingItemAddOns)
    override var subject by Ticket referencedOn TicketBookingItemAddOns.parent

    @get:JsonProperty
    override var addOn by AddOn referencedOn TicketBookingItemAddOns.addon
    @get:JsonProperty
    override var amount : BigDecimal by notifierColumn(
            column = TicketBookingItemAddOns.amount,
            notifier = {
                this.subject.itemAddOnsAmount = null
                this.subject.itemAddOnCount = null
            })
    @get:JsonProperty
    override var selection by TicketBookingItemAddOns.selection
    //@get:JsonProperty
    override var sourceAdjustment by BookingItemAddOn referencedOn TicketBookingItemAddOns.bookingItemAddOn
}
