package ticketpile.service.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntityClass
import ticketpile.service.database.*
import ticketpile.service.util.PrimaryEntity
import ticketpile.service.util.RelationalEntity

/**
 * Created by jonlatane on 8/28/16.
 */

class Booking(id: EntityID<Int>) : PrimaryEntity(id, Bookings), Weighable {
    companion object : IntEntityClass<Booking>(Bookings)
    /*internal val tickets : Iterable<Ticket>
            by children(Ticket, Tickets.booking)*/

    @get:JsonProperty
    val bookingId by PK
    
    @get:JsonProperty 
    var code by Bookings.code
    
    @get:JsonProperty
    var status by Bookings.status
    
    @get:JsonProperty
    val items : Iterable<BookingItem> 
            by children(BookingItem, BookingItems.booking)
    
    @get:JsonProperty
    val addOns : Iterable<BookingAddOn> 
            by children(BookingAddOn)
    
    @get:JsonProperty
    val discounts : Iterable<BookingDiscount> 
            by children(BookingDiscount)
    
    @get:JsonProperty
    val manualAdjustments : Iterable<BookingManualAdjustment> 
            by children(BookingManualAdjustment)
    
    @get:JsonProperty
    var customer by Customer referencedOn Bookings.customer
    
    @get:JsonProperty
    val bookingTotal = grossRevenue
    
    override val tickets : Iterable<Ticket> get() {
        val result = mutableListOf<Ticket>()
        items.forEach {
            it.tickets.forEach { 
                result.add(it)
            }
        }
        return result
    }
}

class BookingAddOn(id: EntityID<Int>) : RelationalEntity(id), AddOnAdjustment<Booking> {
    companion object : IntEntityClass<BookingAddOn>(BookingAddOns)
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
    companion object : IntEntityClass<BookingDiscount>(BookingDiscounts)
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
    companion object : IntEntityClass<BookingManualAdjustment>(BookingManualAdjustments)
    var booking by Booking referencedOn BookingManualAdjustments.parent

    @get:JsonProperty
    val bookingManualAdjustmentId by PK
    @get:JsonProperty
    override var description by BookingManualAdjustments.description
    @get:JsonProperty
    override var amount by BookingManualAdjustments.amount

    override var subject by Booking referencedOn BookingDiscounts.parent
}