package ticketpile.service.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.sql.deleteWhere
import ticketpile.service.database.BookingItemAddOns
import ticketpile.service.database.BookingItems
import ticketpile.service.database.Tickets
import ticketpile.service.model.transformation.Weighable
import ticketpile.service.util.BigZero
import ticketpile.service.util.PrimaryEntity
import ticketpile.service.util.RelationalEntity
import ticketpile.service.util.RelationalEntityClass
import java.math.BigDecimal

/**
 * Corresponds to Advance ResItem/BookingItem
 * Created by jonlatane on 8/28/16.
 */
class BookingItem(id: EntityID<Int>) : PrimaryEntity(id, BookingItems), Weighable {
    companion object : RelationalEntityClass<BookingItem>(BookingItems)
    
    var booking by Booking referencedOn BookingItems.booking
    
    @get:JsonProperty
    var event by Event referencedOn BookingItems.event
    
    @get:JsonProperty
    val bookingItemId by PK
    @get:JsonProperty
    val addOns by BookingItemAddOn childrenOn BookingItemAddOns.parent
    @get:JsonProperty
    override val tickets by Ticket childrenOn Tickets.parent

    @get:JsonProperty
    override var basePrice : BigDecimal? by cacheNotifierColumn(
            column = BookingItems.basePrice,
            calculation = { ticketTotal({it.basePrice}) },
            notifier = {
                this.grossAmount = null
                this.booking.basePrice = null
            })

    @get:JsonProperty
    override var discountsAmount : BigDecimal? by cacheNotifierColumn(
            column = BookingItems.discountsAmount,
            calculation = { ticketTotal({it.discountsAmount!!}) },
            notifier = {
                this.grossAmount = null
                this.booking.discountsAmount = null
            })

    @get:JsonProperty
    override var feesAmount : BigDecimal? by cacheNotifierColumn(
            column = BookingItems.feesAmount,
            calculation = { ticketTotal({it.feesAmount!!}) },
            notifier = {
                this.grossAmount = null
                this.booking.feesAmount = null
            })

    @get:JsonProperty
    override var addOnsAmount : BigDecimal? by cacheNotifierColumn(
            column = BookingItems.addOnsAmount,
            calculation = { ticketTotal({it.addOnsAmount!!}) },
            notifier = {
                this.grossAmount = null
                this.booking.addOnsAmount = null
            })

    @get:JsonProperty
    override var manualAdjustmentsAmount : BigDecimal? by cacheNotifierColumn(
            column = BookingItems.manualAdjustmentsAmount,
            calculation = { ticketTotal({it.manualAdjustmentsAmount!!}) },
            notifier = {
                this.grossAmount = null
                this.booking.manualAdjustmentsAmount = null
            })

    @get:JsonProperty
    override var itemAddOnsAmount : BigDecimal? by cacheNotifierColumn(
            column = BookingItems.itemAddOnsAmount,
            calculation = { adjustmentTotal(addOns) },
            notifier = {
                this.grossAmount = null
                this.booking.itemAddOnsAmount = null
            })

    @get:JsonProperty
    override var grossAmount : BigDecimal? by cacheNotifierColumn(
            column = BookingItems.grossAmount,
            calculation = {
                basePrice!! + discountsAmount!! + feesAmount!! + addOnsAmount!! +
                        manualAdjustmentsAmount!! + itemAddOnsAmount!!
            },
            notifier = {
                this.booking.grossAmount = null
                this.totalAmount = null
            })
    
    @get:JsonProperty
    override var totalAmount: BigDecimal? by cacheNotifierColumn(
            column = BookingItems.totalAmount,
            calculation = {
                BigZero.max(grossAmount)
            },
            notifier = {
                this.booking.totalAmount = null
            })

    fun populateCaches() {
        tickets.forEach(Ticket::populateCaches)
        totalAmount!!
    }
    
    override fun delete() {
        tickets.forEach(Ticket::delete)
        BookingItemAddOns.deleteWhere {
            BookingItemAddOns.parent eq id
        }
        super.delete()
    }

    private fun ticketTotal(operator: (Ticket) -> BigDecimal) : BigDecimal {
        return tickets.map(operator).fold(
                initial = BigZero,
                operation = {
                    amount1, amount2 ->
                    amount1 + amount2
                }
        )
    }
}

class BookingItemAddOn(id: EntityID<Int>) : RelationalEntity(id), AddOnAdjustment<BookingItem> {
    companion object : RelationalEntityClass<BookingItemAddOn>(BookingItemAddOns)

    @get:JsonProperty
    val bookingItemAddOnId by PK
    @get:JsonProperty
    override var addOn by AddOn referencedOn BookingItemAddOns.addon
    @get:JsonProperty
    override var amount by BookingItemAddOns.amount
    @get:JsonProperty
    override var selection by BookingItemAddOns.selection

    override var subject by BookingItem referencedOn BookingItemAddOns.parent
}
