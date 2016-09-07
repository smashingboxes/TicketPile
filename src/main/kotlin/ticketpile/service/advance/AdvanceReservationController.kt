package ticketpile.service.advance

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ticketpile.service.model.Booking

/**
 * Controller capable of receiving Advance reservations and dumping
 * their data into TicketPile.
 * Created by jonlatane on 8/27/16.
 */
@RestController
@RequestMapping(value = "/advance")
open class AdvanceReservationController {
    @PostMapping(value = "/requestImport")
    fun requestImport(
            @RequestParam(value = "advanceHost", required = true)
            host: String,
            @RequestParam(value = "authKey", required = true)
            authorizationKey: String,
            @RequestParam(value = "locationId", required = true)
            locationId: Int,
            @RequestParam(value = "reservationId", required = true)
            reservationId: Int
    ): Booking {
        return AdvanceLocationManager(host, authorizationKey, locationId)
                .importReservation(reservationId)
    }
}