package ticketpile.service.webreserv

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ticketpile.service.model.Booking

/**
 * Controller capable of receiving WebReserv reservations and dumping
 * their data into TicketPile.
 * Created by jonlatane on 8/27/16.
 */
@RestController
@RequestMapping(value = "/webreserv")
open class WebReservationController {
    @PostMapping(value = "/requestImport")
    fun requestImport(
            @RequestParam(value = "webreservHost", required = true)
            host: String,
            @RequestParam(value = "authKey", required = true)
            authorizationKey: String,
            @RequestParam(value = "locationId", required = true)
            locationId: Int,
            @RequestParam(value = "reservationId", required = true)
            reservationId: Int
    ): Booking {
        return WRLocationManager(host, authorizationKey, locationId)
                .importReservation(reservationId)
    }
}