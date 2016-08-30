package ticketpile.service.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.exposed.dao.*
import ticketpile.service.database.*
import ticketpile.service.util.*

/**
 * Created by jonlatane on 8/28/16.
 */

class Booking(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Booking>(Bookings)
    internal val tickets 
            by Children(this, Ticket, Tickets.booking)

    @get:JsonProperty
    val bookingId by IDDelegateOn(this)
    
    @get:JsonProperty 
    var code by Bookings.code
    
    @get:JsonProperty 
    val events : Iterable<BookingEvent> by lazy {
        tickets.groupBy({ it.event }).map {
            it -> BookingEvent(event = it.key, booking = this)
        }
    }
    
    @get:JsonProperty
    val addOns : Iterable<BookingAddOn> 
            by Children(this, BookingAddOn, BookingAddOns.parent)
    @get:JsonProperty
    val discounts : Iterable<BookingDiscount> 
            by Children(this, BookingDiscount, BookingDiscounts.parent)
    @get:JsonProperty
    val manualAdjustments : Iterable<BookingManualAdjustment> 
            by Children(this, BookingManualAdjustment, BookingManualAdjustments.parent)
}

class BookingAddOn(id: EntityID<Int>) : IntEntity(id), AddOnAdjustment<Booking> {
    companion object : IntEntityClass<BookingAddOn>(BookingAddOns)
    var booking by Booking referencedOn BookingAddOns.parent

    @get:JsonProperty
    val bookingAddOnId by IDDelegateOn(this)
    @get:JsonProperty
    override var addOn by AddOn referencedOn BookingAddOns.addon
    @get:JsonProperty
    override var amount by BookingAddOns.amount
    
    override var subject by Booking referencedOn BookingAddOns.parent
}

class BookingDiscount(id: EntityID<Int>) : IntEntity(id), DiscountAdjustment<Booking> {
    companion object : IntEntityClass<BookingDiscount>(BookingDiscounts)
    internal var booking by Booking referencedOn BookingDiscounts.parent

    @get:JsonProperty
    val bookingDiscountId by IDDelegateOn(this)
    @get:JsonProperty
    override var discount by Discount referencedOn BookingDiscounts.discount
    @get:JsonProperty
    override var amount by BookingDiscounts.amount
    
    override var subject by Booking referencedOn BookingDiscounts.parent
}

class BookingManualAdjustment(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<BookingManualAdjustment>(BookingManualAdjustments)
    var booking by Booking referencedOn BookingManualAdjustments.parent

    @get:JsonProperty
    val bookingManualAdjustmentId by IDDelegateOn(this)
    @get:JsonProperty
    var description by BookingManualAdjustments.description
    @get:JsonProperty
    var amount by BookingManualAdjustments.amount
}