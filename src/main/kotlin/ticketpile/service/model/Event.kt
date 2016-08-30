package ticketpile.service.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import ticketpile.service.database.*
import ticketpile.service.util.Children
import ticketpile.service.util.IDDelegateOn

/**
 * Created by jonlatane on 8/28/16.
 */
open class Event(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Event>(Events)
    internal val tickets by Children(this, Ticket, Tickets.event)

    @get:JsonProperty
    val eventId by IDDelegateOn(this)
    @get:JsonProperty
    var startTime by Events.startTime
    @get:JsonProperty
    var endTime by Events.endTime
    @get:JsonProperty
    var product by Product referencedOn Events.product
    @get:JsonProperty
    var capacity by Events.capacity
    @get:JsonProperty
    val availability by lazy { capacity - tickets.count() }
}

class BookingEvent(val booking: Booking, val event: Event) {
    val tickets = booking.tickets.filter { 
        ticket -> ticket.event.id.equals(event.id)
    }
    val addOns = EventAddOns.select {
        EventAddOns.parent.eq(event.id) and EventAddOns.booking.eq(booking.id)
    }.map {
        it -> EventAddOn(it[EventAddOns.id])
    }
}

class EventAddOn(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<EventAddOn>(EventAddOns)
    val event by EventAddOns.parent
    val booking by EventAddOns.booking

    @get:JsonProperty
    val eventAddOnId by IDDelegateOn(this)
    @get:JsonProperty
    val addOnSelection by AddOn referencedOn EventAddOns.addon
    @get:JsonProperty
    val amount by EventAddOns.amount
}