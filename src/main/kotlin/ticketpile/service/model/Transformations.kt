package ticketpile.service.model

import org.jetbrains.exposed.sql.deleteWhere
import ticketpile.service.database.TicketBookingAddOns
import ticketpile.service.database.TicketBookingManualAdjustments
import java.math.BigDecimal

/**
 * Created by jonlatane on 9/25/16.
 */ 
internal val transforms = arrayOf(
        BookingManualAdjustmentTransformation,
        BookingAddOnTransformation,
        BookingItemAddOnTransformation
)
interface Weighable {
    val tickets : Iterable<Ticket>
    val grossRevenue : BigDecimal get() {
        var result = BigDecimal.ZERO
        tickets.forEach {
            result += it.grossRevenue
        }
        return result
    }
}
val weighByTicketCount = {
    weighable: Weighable, ticket: Ticket ->
    BigDecimal.ONE / 
            BigDecimal(weighable.tickets.count())
}
val weighByGrossRevenue = {
    weighable: Weighable, ticket: Ticket ->
    ticket.grossRevenue /
            weighable.grossRevenue
}
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
            _transform(booking)
        }
    }
}

internal object BookingAddOnTransformation : TicketAdjustmentTransform<BookingAddOn>() {
    override fun prepare(ticket : Ticket) {
        TicketBookingAddOns.deleteWhere { 
            TicketBookingAddOns.parent eq ticket.id
        }
    }
    override fun transform(source: BookingAddOn) {
        val booking = source.booking
        booking.tickets.forEach { 
            ticket ->
            TicketBookingAddOn.new { 
                subject = ticket
                sourceAdjustment = source
                addOn = source.addOn
                amount = source.amount * weighByGrossRevenue(booking, ticket)
                prompt = source.prompt
            }
        }
    }
    override fun sources(booking : Booking) : Iterable<BookingAddOn> {
        return booking.addOns
    }
}

internal object BookingManualAdjustmentTransformation : TicketAdjustmentTransform<BookingManualAdjustment>() {
    override fun prepare(ticket : Ticket) {
        TicketBookingManualAdjustments.deleteWhere {
            TicketBookingManualAdjustments.parent eq ticket.id
        }
    }
    
    override fun transform(source: BookingManualAdjustment) {
        val booking = source.booking
        booking.tickets.forEach {
            ticket ->
            TicketBookingManualAdjustment.new {
                subject = ticket
                sourceAdjustment = source
                description = source.description
                amount = source.amount * weighByGrossRevenue(booking, ticket)
            }
        }
    }
    override fun sources(booking : Booking) : Iterable<BookingManualAdjustment> {
        return booking.manualAdjustments
    }
}

internal object BookingItemAddOnTransformation : TicketAdjustmentTransform<BookingItemAddOn>() {
    override fun prepare(ticket : Ticket) {
        TicketBookingAddOns.deleteWhere {
            TicketBookingAddOns.parent eq ticket.id
        }
    }
    override fun transform(source: BookingItemAddOn) {
        val bookingItem = source.subject
        bookingItem.tickets.forEach {
            ticket ->
            TicketBookingItemAddOn.new {
                subject = ticket
                sourceAdjustment = source
                addOn = source.addOn
                amount = source.amount * weighByGrossRevenue(bookingItem, ticket)
                prompt = source.prompt
            }
        }
    }
    override fun sources(booking : Booking) : Iterable<BookingItemAddOn> {
        val result = mutableListOf<BookingItemAddOn>()
        booking.items.forEach { 
            it.addOns.forEach { 
                result.add(it)
            }
        }
        return result
    }
}