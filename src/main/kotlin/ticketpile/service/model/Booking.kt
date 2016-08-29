package ticketpile.service.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.exposed.dao.*
import ticketpile.service.database.*
import kotlin.reflect.KProperty

/**
 * Created by jonlatane on 8/28/16.
 */

class Booking(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Booking>(Bookings)
    internal val tickets
            by Ticket.referrersOn(Tickets.booking, cache = true)

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
            by BookingAddOn.referrersOn(BookingAddOns.parent, cache = true)
    @get:JsonProperty
    val discounts : Iterable<BookingDiscount> 
            by BookingDiscount.referrersOn(BookingDiscounts.parent, cache = true)
    @get:JsonProperty
    val manualAdjustments : Iterable<BookingManualAdjustment>
            by BookingManualAdjustment.referrersOn(BookingManualAdjustments.parent, cache = true)
}

class BookingAddOn(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<BookingAddOn>(BookingAddOns)
    var booking by BookingAddOns.parent
    val addOnSelection by AddOn referencedOn BookingAddOns.addon
    val amount by BookingAddOns.amount
}

class BookingDiscount(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<BookingDiscount>(BookingDiscounts)
    var booking by BookingDiscounts.parent
    val discount by BookingDiscounts.discount
    val amount by BookingDiscounts.amount
}

class BookingManualAdjustment(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<BookingManualAdjustment>(BookingManualAdjustments)
    var booking by BookingManualAdjustments.parent
    val description by BookingManualAdjustments.description
    val amount by BookingManualAdjustments.amount
}