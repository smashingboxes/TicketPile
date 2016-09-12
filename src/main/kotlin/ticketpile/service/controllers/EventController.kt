package ticketpile.service

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ticketpile.service.controllers.AdjustmentController
import ticketpile.service.model.Event
import ticketpile.service.util.transaction

@RestController
@RequestMapping(value = "/event")
class EventController() : AdjustmentController<Event>(Event) {
    /*
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
    */
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
}