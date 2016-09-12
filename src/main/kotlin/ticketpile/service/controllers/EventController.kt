package ticketpile.service

import ticketpile.service.util.transaction
import org.joda.time.DateTime
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import ticketpile.service.controllers.AdjustmentController
import ticketpile.service.model.Event
import ticketpile.service.model.Product

@RestController
@RequestMapping(value = "/event")
class EventController() : AdjustmentController<Event>(Event) {
    @PostMapping(
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

    @GetMapping(value = "/all",
            produces = arrayOf(MediaType.APPLICATION_JSON_UTF8_VALUE)
    )
    fun allEvents() : Iterable<Event> {
        var allEvents : Iterable<Event> = emptyList()
        transaction {
            allEvents = Event.all().map {it -> it}
        }
        return allEvents
    }

    /*
    @PostMapping(value = "/{eventId}/addOn/ ")
    override fun applyEventAddOn(
            @RequestParam(value = "bookingId", required = true)
            bookingId: Int,
            @PathVariable(value = "eventId")
            eventId: Int,
            @RequestParam(value = "addOnId", required = true)
            addOnId: Int,
            @RequestParam(value = "addOnAmount", required = true)
            addOnAmount: BigDecimal
    ): EventAddOn {
        return super.applyEventAddOn(bookingId,  eventId, addOnId, addOnAmount)
    }

    @DeleteMapping(value = "/{eventId}/addon/{bookingAddOnId}")
    override fun deleteEventAddOn(
            @RequestParam(value = "bookingId", required = false)
            bookingId: Int?,
            @PathVariable(value = "eventId")
            eventId: Int?,
            @RequestParam(value = "addOnId", required = true)
            eventAddOnId: Int
    ): BookingEvent? {
        return deleteEventAddOn(bookingId, eventId, eventAddOnId)
    }*/
}