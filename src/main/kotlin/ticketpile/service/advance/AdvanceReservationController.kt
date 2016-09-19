package ticketpile.service.advance

import AdvanceSyncTask
import org.springframework.web.bind.annotation.*
import ticketpile.service.model.Booking
import ticketpile.service.util.transaction

/**
 * Controller capable of receiving Advance reservations and dumping
 * their data into TicketPile.
 * Created by jonlatane on 8/27/16.
 */
@RestController
@RequestMapping(value = "/advance")
open class AdvanceReservationController {
    @PostMapping(value = "/singleImport")
    fun singleImport(
            @RequestParam(value = "advanceHost", required = true)
            host: String,
            @RequestParam(value = "advanceAuthKey", required = true)
            authorizationKey: String,
            @RequestParam(value = "advanceLocationId", required = true)
            locationId: Int,
            @RequestParam(value = "reservationId", required = true)
            reservationId: Int
    ): Booking {
        return AdvanceLocationManager(host, authorizationKey, locationId)
                .importById(reservationId)
    }
    
    @PostMapping(value = "/bulkImport")
    fun bulkImport(
            @RequestParam(value = "advanceHost", required = true)
            host: String,
            @RequestParam(value = "advanceAuthKey", required = true)
            authorizationKey: String,
            @RequestParam(value = "advanceLocationId", required = true)
            locationId: Int
    ) : AdvanceSyncTask {
        val manager = AdvanceLocationManager(host, authorizationKey, locationId)
        return manager.queueAllBookingsForImport()
    }
    
    @GetMapping(value = "/importQueue")
    fun importQueue() : Iterable<AdvanceSyncTask> {
        return transaction {
            AdvanceSyncTask.all().map {it}
        }
    }
}