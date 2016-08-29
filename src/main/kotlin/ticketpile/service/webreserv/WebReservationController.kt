package ticketpile.service.webreserv

import org.springframework.web.bind.annotation.*
import ticketpile.service.model.Booking

/**
 * Controller capable of receiving WebReserv reservations and dumping
 * their data into TicketPile.
 * Created by jonlatane on 8/27/16.
 */
@RestController
@RequestMapping(value = "/webreserv")
class WebReservationController {
    @RequestMapping(value = "/booking", method = arrayOf(RequestMethod.POST))
    fun import(@RequestBody booking : WebReservation): Booking
    {
        val booking = Booking.new {

        }
        return booking
    }
}
