package ticketpile.service.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntityClass
import ticketpile.service.database.BookingItems
import ticketpile.service.database.Events
import ticketpile.service.util.PrimaryEntity

/**
 * Created by jonlatane on 8/28/16.
 */
open class Event(id: EntityID<Int>) : PrimaryEntity(id, Events) {
    companion object : IntEntityClass<Event>(Events)

    @get:JsonProperty
    val eventId by PK
    @get:JsonProperty
    var startTime by Events.startTime
    @get:JsonProperty
    var endTime by Events.endTime
    @get:JsonProperty
    var product by Product referencedOn Events.product
    @get:JsonProperty
    var capacity by Events.capacity
    @get:JsonProperty
    val availability : Int by lazy { 
        var utilization = 0
        items.forEach { 
            utilization += it.tickets.count()
        }
        capacity - utilization
    }
    @get:JsonProperty
    val items by BookingItem referrersOn BookingItems.event
}

/*class BookingEvent(val booking: Booking, val event: Event) {
    val tickets = booking.tickets.filter { 
        ticket ->
        ticket.event.id == event.id
    }
    val addOns = BookingItemAddOns.select {
        BookingItemAddOns.parent.eq(event.id) and BookingItemAddOns.booking.eq(booking.id)
    }.map {
        it -> EventAddOn(it[BookingItemAddOns.id])
    }
}*/
/*
class EventAddOn(id: EntityID<Int>) : RelationalEntity(id) {
    companion object : IntEntityClass<EventAddOn>(BookingItemAddOns)
    var event by Event referencedOn BookingItemAddOns.parent
    var booking by Booking referencedOn BookingItemAddOns.booking

    @get:JsonProperty
    val eventAddOnId by PK
    @get:JsonProperty
    var addOn by AddOn referencedOn BookingItemAddOns.addon
    @get:JsonProperty
    var amount by BookingItemAddOns.amount
}*/