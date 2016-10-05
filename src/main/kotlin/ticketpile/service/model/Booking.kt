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

class Booking(id: EntityID<Int>) : PrimaryEntity(id, Bookings), Weighable {
    companion object : IntEntityClass<Booking>(Bookings)

    @get:JsonProperty
    val bookingId by PK
    
    @get:JsonProperty 
    var code by Bookings.code
    
    @get:JsonProperty
    var status by Bookings.status
    
    @get:JsonProperty
    val items by BookingItem childrenOn BookingItems.booking
    
    @get:JsonProperty
    val addOns by BookingAddOn childrenOn BookingAddOns.parent
    
    @get:JsonProperty
    val discounts by BookingDiscount childrenOn BookingDiscounts.parent
    
    @get:JsonProperty
    val manualAdjustments by BookingManualAdjustment childrenOn BookingManualAdjustments.parent
    
    @get:JsonProperty
    var customer by Customer referencedOn Bookings.customer
    
    @get:JsonProperty
    val bookingTotal :BigDecimal get() {
        return grossRevenue
    }
    
    override val tickets : List<Ticket> get() {
        val result = mutableListOf<Ticket>()
        items.forEach {
            it.tickets.forEach { 
                result.add(it)
            }
        }
        return result
    }
    
    var matchesExternal by Bookings.matchesExternal
    
    override fun delete() {
        items.forEach {
            bookingItem ->
            bookingItem.tickets.forEach {
                ticket ->
                TicketBookingAddOns.deleteWhere {
                    TicketBookingAddOns.parent eq ticket.id
                }
                TicketBookingDiscounts.deleteWhere {
                    TicketBookingDiscounts.parent eq ticket.id
                }
                TicketBookingManualAdjustments.deleteWhere {
                    TicketBookingManualAdjustments.parent eq ticket.id
                }
                TicketBookingItemAddOns.deleteWhere {
                    TicketBookingItemAddOns.parent eq ticket.id
                }
                ticket.delete()
            }
            BookingItemAddOns.deleteWhere {
                BookingItemAddOns.parent eq bookingItem.id
            }
            bookingItem.delete()
        }
        BookingAddOns.deleteWhere {
            BookingAddOns.parent eq id
        }
        BookingDiscounts.deleteWhere {
            BookingDiscounts.parent eq id
        }
        BookingManualAdjustments.deleteWhere {
            BookingManualAdjustments.parent eq id
        }
        super.delete()
    }
}

class BookingAddOn(id: EntityID<Int>) : RelationalEntity(id), AddOnAdjustment<Booking> {
    companion object : RelationalEntityClass<BookingAddOn>(BookingAddOns)
    var booking by Booking referencedOn BookingAddOns.parent

    @get:JsonProperty
    val bookingAddOnId by PK
    @get:JsonProperty
    override var addOn by AddOn referencedOn BookingAddOns.addon
    @get:JsonProperty
    override var amount by BookingAddOns.amount
    @get:JsonProperty
    override var prompt by BookingAddOns.prompt
    
    override var subject by Booking referencedOn BookingAddOns.parent
}

class BookingDiscount(id: EntityID<Int>) : RelationalEntity(id), DiscountAdjustment<Booking> {
    companion object : RelationalEntityClass<BookingDiscount>(BookingDiscounts)
    internal var booking by Booking referencedOn BookingDiscounts.parent

    @get:JsonProperty
    val bookingDiscountId by PK
    @get:JsonProperty
    override var discount by Discount referencedOn BookingDiscounts.discount
    @get:JsonProperty
    override var amount by BookingDiscounts.amount
    
    override var subject by Booking referencedOn BookingDiscounts.parent
}

class BookingManualAdjustment(id: EntityID<Int>) : RelationalEntity(id), ManualAdjustment<Booking> {
    companion object : RelationalEntityClass<BookingManualAdjustment>(BookingManualAdjustments)
    var booking by Booking referencedOn BookingManualAdjustments.parent

    @get:JsonProperty
    val bookingManualAdjustmentId by PK
    @get:JsonProperty
    override var description by BookingManualAdjustments.description
    @get:JsonProperty
    override var amount by BookingManualAdjustments.amount

    override var subject by Booking referencedOn BookingManualAdjustments.parent
}