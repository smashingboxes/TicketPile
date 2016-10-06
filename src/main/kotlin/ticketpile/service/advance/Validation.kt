package ticketpile.service.advance

import ticketpile.service.model.Booking
import java.math.BigDecimal

/**
 * A few simple methods for validating TicketPile bookings against Advance reservations.
 * 
 * Created by jonlatane on 10/5/16.
 */

class AdvanceValidationException(message : String, reservation : AdvanceReservation) : Exception(
        "Reservation ${reservation.bookingCode}: $message"
) {
    init {
        println("Validation Error on ${reservation.bookingCode}: $message")
    }
}

fun isValid(booking : Booking, reservation : AdvanceReservation) : Boolean {
    try {
        validate(booking, reservation)
    } catch(e : AdvanceValidationException) {
        return false
    }
    return true
}

private fun validate(booking : Booking, reservation : AdvanceReservation) {
    //Test booking item count
    if(booking.items.count() != reservation.bookingItems.count())
        throw AdvanceValidationException("Booking item count did not match: ${booking.items.count()} " +
                "vs ${reservation.bookingItems.count()}", reservation)
    
    //Test ticket count
    if(booking.tickets.count() != reservation.bookingItems.fold(0, {
        bookingTicketTotal, advanceBookingItem ->
        bookingTicketTotal + advanceBookingItem.lineTotals.filter { it.type == 1101 }.fold(0, {
            itemTicketTotal, lineTotal ->
            itemTicketTotal + lineTotal.quantity
        })
    }))
        throw AdvanceValidationException("Ticket count did not match", reservation)
    
    //Test booking total
    if((booking.bookingTotal - reservation.pricing.totalAmount).abs() > BigDecimal(0.00001))
        throw AdvanceValidationException("Booking total did not match", reservation)
    
    println("Validation success for ${reservation.bookingCode}!")
}