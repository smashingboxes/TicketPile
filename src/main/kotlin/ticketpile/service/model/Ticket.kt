package ticketpile.service.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.sql.deleteWhere
import ticketpile.service.database.*
import ticketpile.service.util.PrimaryEntity
import ticketpile.service.util.RelationalEntity
import ticketpile.service.util.RelationalEntityClass
import java.math.BigDecimal

/**
 * Created by jonlatane on 8/28/16.
 */
class Ticket(id: EntityID<Int>) : PrimaryEntity(id, Tickets) {
    companion object : RelationalEntityClass<Ticket>(Tickets)
    var bookingItem by BookingItem referencedOn Tickets.parent
    
    @get:JsonProperty
    val ticketId by PK
    @get:JsonProperty
    var code by Tickets.code
    @get:JsonProperty
    var personCategory by PersonCategory referencedOn Tickets.personCategory
    @get:JsonProperty
    var basePrice by Tickets.basePrice
    //@get:JsonProperty
    val bookingAddOnAdjustments by TicketBookingAddOn childrenOn TicketBookingAddOns.parent
    //@get:JsonProperty
    val bookingManualAdjustments by TicketBookingManualAdjustment childrenOn TicketBookingManualAdjustments.parent
    //@get:JsonProperty
    val bookingDiscountAdjustments by TicketBookingDiscount childrenOn TicketBookingDiscounts.parent
    //@get:JsonProperty
    val bookingItemAddOnAdjustments by TicketBookingItemAddOn childrenOn TicketBookingItemAddOns.parent
    @get:JsonProperty
    val grossRevenue : BigDecimal get() {
        var result = basePrice
        for(adjustments in listOf<Iterable<Adjustment<*>>>(
                bookingAddOnAdjustments,
                bookingManualAdjustments,
                bookingDiscountAdjustments,
                bookingItemAddOnAdjustments
        )) {
            for(adjustment in adjustments) {
                result += adjustment.amount
            }
        }
        return result
    }
    @get:JsonProperty
    val discountedPrice : BigDecimal get() {
        var result = basePrice
        for(adjustment in bookingDiscountAdjustments) {
            result += adjustment.amount
        }
        return result
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
        super.delete()
    }
}

class PersonCategory(id: EntityID<Int>) : PrimaryEntity(id, PersonCategories) {
    companion object : IntEntityClass<PersonCategory>(PersonCategories)

    @get:JsonProperty
    val personCategoryId by PK
    @get:JsonProperty
    var name by PersonCategories.name
    @get:JsonProperty
    var description by PersonCategories.description
}

class TicketBookingAddOn(id: EntityID<Int>) : RelationalEntity(id), AddOnAdjustment<Ticket>, MappedAdjustment<BookingAddOn>{
    companion object : RelationalEntityClass<TicketBookingAddOn>(TicketBookingAddOns)
    override var subject by Ticket referencedOn TicketBookingAddOns.parent
    
    @get:JsonProperty
    override var addOn by AddOn referencedOn TicketBookingAddOns.addon
    @get:JsonProperty
    override var amount by TicketBookingAddOns.amount
    @get:JsonProperty
    override var prompt by TicketBookingAddOns.prompt
    //@get:JsonProperty
    override var sourceAdjustment by BookingAddOn referencedOn TicketBookingAddOns.bookingAddOn
}

class TicketBookingDiscount(id: EntityID<Int>) : RelationalEntity(id), DiscountAdjustment<Ticket>, MappedAdjustment<BookingDiscount> {
    companion object : RelationalEntityClass<TicketBookingDiscount>(TicketBookingDiscounts)
    override var subject by Ticket referencedOn TicketBookingDiscounts.parent

    @get:JsonProperty
    override var discount by Discount referencedOn TicketBookingDiscounts.discount
    @get:JsonProperty
    override var amount by TicketBookingDiscounts.amount
    //@get:JsonProperty
    override var sourceAdjustment by BookingDiscount referencedOn TicketBookingDiscounts.bookingDiscount
}

class TicketBookingManualAdjustment(id: EntityID<Int>) : RelationalEntity(id), ManualAdjustment<Ticket>, MappedAdjustment<BookingManualAdjustment> {
    companion object : RelationalEntityClass<TicketBookingManualAdjustment>(TicketBookingManualAdjustments)
    override var subject by Ticket referencedOn TicketBookingManualAdjustments.parent

    @get:JsonProperty
    override var description by TicketBookingManualAdjustments.description
    @get:JsonProperty
    override var amount by TicketBookingManualAdjustments.amount
    //@get:JsonProperty
    override var sourceAdjustment by BookingManualAdjustment referencedOn TicketBookingManualAdjustments.bookingManualAdjustment
}

class TicketBookingFee(id: EntityID<Int>) : RelationalEntity(id), ManualAdjustment<Ticket>, MappedAdjustment<BookingFee> {
    companion object : RelationalEntityClass<TicketBookingFee>(TicketBookingFees)
    override var subject by Ticket referencedOn TicketBookingFees.parent

    @get:JsonProperty
    override var description by TicketBookingFees.description
    @get:JsonProperty
    override var amount by TicketBookingFees.amount
    //@get:JsonProperty
    override var sourceAdjustment by BookingFee referencedOn TicketBookingFees.bookingFee
}

class TicketBookingItemAddOn(id: EntityID<Int>) : RelationalEntity(id), AddOnAdjustment<Ticket>, MappedAdjustment<BookingItemAddOn> {
    companion object : RelationalEntityClass<TicketBookingItemAddOn>(TicketBookingItemAddOns)
    override var subject by Ticket referencedOn TicketBookingItemAddOns.parent

    @get:JsonProperty
    override var addOn by AddOn referencedOn TicketBookingItemAddOns.addon
    @get:JsonProperty
    override var amount by TicketBookingItemAddOns.amount
    @get:JsonProperty
    override var prompt by TicketBookingItemAddOns.prompt
    //@get:JsonProperty
    override var sourceAdjustment by BookingItemAddOn referencedOn TicketBookingItemAddOns.bookingItemAddOn
}
