package ticketpile.service.controllers;

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import ticketpile.service.database.Bookings
import ticketpile.service.model.Booking
import java.util.*

/**
 * Created by jonlatane on 8/28/16.
 */
@RestController
class BookingController {
    @PostMapping(value = "/booking", 
            produces = arrayOf(MediaType.APPLICATION_JSON_UTF8_VALUE)
    )
    fun createBooking(
            @RequestParam(value = "code", required = false)
            bookingCode: String?
    ): Booking
    {
        val nonNullCode : String
        if(bookingCode == null) {
            nonNullCode = UUID.randomUUID().toString()
        } else {
            nonNullCode = bookingCode
        }
        var booking : Booking? = null
        transaction {
            booking = Booking.new {
                code = nonNullCode
            }
        }
        return booking!!
    }

    @GetMapping(value = "/booking/all",
            produces = arrayOf(MediaType.APPLICATION_JSON_UTF8_VALUE)
    )
    fun allBookings() : Iterable<Booking> {
        var allBookings : Iterable<Booking> = emptyList()
        transaction {
            allBookings = Booking.all().map {it -> it}
        }
        return allBookings
    }
}
