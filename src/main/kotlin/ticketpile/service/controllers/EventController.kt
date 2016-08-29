package ticketpile.service

import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import ticketpile.service.model.Booking
import ticketpile.service.model.Event
import ticketpile.service.model.Product
import java.util.*
import java.util.concurrent.atomic.AtomicLong

@RestController
class EventController {
    @PostMapping(value = "/event",
            produces = arrayOf(MediaType.APPLICATION_JSON_UTF8_VALUE)
    )
    fun createEvent(
            @RequestParam(value = "startTime", required = true)
            @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)
            start: DateTime,
            @RequestParam(value = "endTime", required = true)
            @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)
            end: DateTime,
            @RequestParam(value = "productId", required = true)
            productId: Int,
            @RequestParam(value = "capacity", required = true)
            cap: Int
            ): Event
    {
        var event : Event? = null
        transaction {
            event = Event.new {
                startTime = start
                endTime = end
                product = Product.findById(productId)!!
                capacity = cap
            }
        }
        return event!!
    }

    @GetMapping(value = "/event/all",
            produces = arrayOf(MediaType.APPLICATION_JSON_UTF8_VALUE)
    )
    fun allEvents() : Iterable<Event> {
        var allEvents : Iterable<Event> = emptyList()
        transaction {
            allEvents = Event.all().map {it -> it}
        }
        return allEvents
    }
}