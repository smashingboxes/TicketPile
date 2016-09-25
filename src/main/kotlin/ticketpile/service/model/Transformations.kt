package ticketpile.service.model

import java.math.BigDecimal

/**
 * Created by jonlatane on 9/25/16.
 */ 
internal val transforms = arrayOf(
        BookingAddOnTransformation,
        BookingManualAdjustmentTransformation
)
abstract class TicketAdjustmentTransform<Adj : Adjustment<*>>() {
    abstract fun prepare(ticket : Ticket)
    abstract fun transform(source : Adj)
    abstract fun sources(booking : Booking) : Iterable<Adj>
    private fun transform(booking : Booking) {
        sources(booking).forEach { 
            adjustment ->
            transform(adjustment)
        }
    }
    
    companion object {
        private fun prepare(booking : Booking) {
            booking.tickets.forEach {
                ticket ->
                transforms.forEach {
                    it.prepare(ticket)
                }
            }
        }
        private fun _transform(booking : Booking) {
            transforms.forEach { 
                it.transform(booking)
            }
        }
        fun transform(booking : Booking) {
            prepare(booking)
            transform(booking)
        }
    }
}

internal object BookingAddOnTransformation : TicketAdjustmentTransform<BookingAddOn>() {
    override fun prepare(ticket : Ticket) {
        ticket.bookingAddOnAdjustments.forEach(TicketBookingAddOn::delete)
    }
    override fun transform(bookingAddOn : BookingAddOn) {
        val booking = bookingAddOn.booking
        booking.tickets.forEach { 
            ticket ->
            TicketBookingAddOn.new { 
                subject = ticket
                sourceAdjustment = bookingAddOn
                addOn = bookingAddOn.addOn
                amount = bookingAddOn.amount *
                        ticket.grossRevenue /
                        booking.grossRevenue
            }
        }
    }
    override fun sources(booking : Booking) : Iterable<BookingAddOn> {
        return booking.addOns
    }
}

internal object BookingManualAdjustmentTransformation : TicketAdjustmentTransform<BookingManualAdjustment>() {
    override fun prepare(ticket : Ticket) {
        ticket.bookingManualAdjustments.forEach(TicketBookingManualAdjustment::delete)
    }
    
    override fun transform(bookingManualAdjustment: BookingManualAdjustment) {
        val booking = bookingManualAdjustment.booking
        booking.tickets.forEach {
            ticket ->
            TicketBookingManualAdjustment.new {
                subject = ticket
                sourceAdjustment = bookingManualAdjustment
                description = bookingManualAdjustment.description
                amount = bookingManualAdjustment.amount *
                        BigDecimal.ONE /
                        BigDecimal(booking.tickets.count())
            }
        }
    }
    override fun sources(booking : Booking) : Iterable<BookingManualAdjustment> {
        return booking.manualAdjustments
    }
}