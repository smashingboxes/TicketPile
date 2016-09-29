package ticketpile.service.controllers;

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ticketpile.service.model.Booking
import ticketpile.service.util.BadRequestException
import ticketpile.service.util.transaction

/**
 * Controller for managing Bookings.
 * 
 * Created by jonlatane on 8/28/16.
 */
@RestController
@RequestMapping(value = "/booking")
class BookingController : AdjustmentController<Booking>(Booking) {
    /*
    @PostMapping(
            produces = arrayOf(MediaType.APPLICATION_JSON_UTF8_VALUE)
    )
    fun createBooking(
            @RequestParam(value = "code", required = false)
            bookingCode: String?
    ): Booking {
        val nonNullCode: String
        if (bookingCode == null) {
            nonNullCode = UUID.randomUUID().toString()
        } else {
            nonNullCode = bookingCode
        }
        var booking: Booking? = null
        transaction {
            booking = Booking.new {
                code = nonNullCode
            }
        }
        return booking!!
    }
    */
    
    @GetMapping(
            value = "/{bookingId}",
            produces = arrayOf(MediaType.APPLICATION_JSON_UTF8_VALUE)
    )
    fun getBooking(
            @PathVariable("bookingId")
            bookingId: Int
    ) : Booking {
        return transaction {
            Booking.findById(bookingId)
        } ?: throw BadRequestException("Booking could not be found")
    }

    @GetMapping(
            value = "/all",
            produces = arrayOf(MediaType.APPLICATION_JSON_UTF8_VALUE)
    )
    fun allBookings(): Iterable<Booking> {
        var allBookings: Iterable<Booking> = emptyList()
        transaction {
            allBookings = Booking.all().map { it -> it }
        }
        return allBookings
    }

    /*
    @PostMapping(value = "/{bookingId}/discount")
    fun applyDiscount(
            @PathVariable("bookingId")
            bookingId: Int,
            @RequestParam(value = "discountId")
            discountId: Int,
            @RequestParam(value = "amount")
            discountAmount: BigDecimal
    ): BookingDiscount {
        return applyDiscount(
                subjectId = bookingId,
                discountId = discountId,
                discountAmount = discountAmount,
                adjustmentClass = BookingDiscount
        )
    }

    @DeleteMapping(value = "/{bookingId}/discount/{bookingDiscountId}")
    fun deleteDiscount(
            @PathVariable("bookingId")
            bookingId: Int,
            @PathVariable("bookingDiscountId")
            bookingDiscountId: Int
    ): Booking {
        return deleteDiscount(
                subjectId = bookingId,
                adjustmentId = bookingDiscountId,
                adjustmentClass = BookingDiscount
        )
    }

    @PostMapping(value = "/{bookingId}/addon")
    fun applyAddOn(
            @PathVariable("bookingId")
            bookingId: Int,
            @RequestParam(value = "addOnId")
            addOnId: Int,
            @RequestParam(value = "amount")
            addOnAmount: BigDecimal
    ): BookingAddOn {
        return applyAddOn(
                subjectId = bookingId,
                addOnId = addOnId,
                addOnAmount = addOnAmount,
                adjustmentClass = BookingAddOn
        )
    }

    @DeleteMapping(value = "/{bookingId}/addon/{bookingAddOnId}")
    fun deleteAddOn(
            @PathVariable("bookingId")
            bookingId: Int,
            @PathVariable("bookingAddOnId")
            bookingAddOnId: Int
    ): Booking {
        return deleteAddOn(
                subjectId = bookingId,
                adjustmentId = bookingAddOnId,
                adjustmentClass = BookingAddOn
        )
    }
    */
}