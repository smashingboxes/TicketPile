package ticketpile.service.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntityClass
import ticketpile.service.database.BookingItems
import ticketpile.service.database.Events
import ticketpile.service.util.PrimaryEntity

/**
 * Corresponds to an Availability in Advance.
 * 
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
    val items by BookingItem referrersOn BookingItems.event
    @get:JsonProperty
    var locationId by Events.locationId
}