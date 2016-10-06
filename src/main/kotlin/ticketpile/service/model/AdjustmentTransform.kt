package ticketpile.service.model

import org.jetbrains.exposed.sql.deleteWhere
import ticketpile.service.database.TicketBookingAddOns
import ticketpile.service.database.TicketBookingDiscounts
import ticketpile.service.database.TicketBookingManualAdjustments
import java.math.BigDecimal

/**
 * Types and logic responsible for transforming Booking and BookingItem
 * adjustments onto tickets.
 * 
 * Created by jonlatane on 9/25/16.
 */ 
internal val transforms = arrayOf(
        BookingDiscountTransformation,
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
val weighByApplicableTicketCount = {
    amount : BigDecimal, weighable: Weighable, ticket: Ticket, applicable: (Ticket) -> Boolean ->
    if(applicable(ticket))
        amount / BigDecimal(weighable.tickets.filter(applicable).count())
        //BigDecimal.ONE.setScale(30, RoundingMode.HALF_DOWN) / 
        //        (BigDecimal(weighable.tickets.filter(applicable).count())
        //                .setScale(30, RoundingMode.HALF_DOWN))
    else
        BigDecimal.ZERO
}
val weighByApplicableGrossRevenue = {
    amount : BigDecimal, weighable: Weighable, ticket: Ticket, applicable: (Ticket) -> Boolean ->
    if(applicable(ticket)) {
        val applicableGross = weighable.tickets.filter(applicable).fold(BigDecimal.ZERO, {
            total, ticket ->
            total + ticket.grossRevenue
        })
        if(applicableGross == BigDecimal.ZERO.setScale(applicableGross.scale()))
            weighByApplicableTicketCount(amount, weighable, ticket, applicable)
        else 
            amount *
                    ticket.grossRevenue /
                    applicableGross
    } else
        BigDecimal.ZERO
}
abstract class TicketAdjustmentTransform<Adj : Adjustment<*>>() {
    abstract fun prepare(ticket : Ticket)
    abstract fun weigh(source : Adj, ticket : Ticket) : BigDecimal
    abstract fun transform(source : Adj, ticket: Ticket, weighedAmount: BigDecimal)
    abstract fun sources(booking : Booking) : Iterable<Adj>
    open fun applicability(source : Adj) : (Ticket) -> Boolean {
        return {
            ticket : Ticket ->
            true
        }
    }
    private fun transform(booking : Booking) {
        sources(booking).forEach {
            adjustment ->
            val tickets = booking.tickets
            val weighedValues = mutableMapOf<Ticket, BigDecimal>()
            tickets.forEach {
                weighedValues[it] = weigh(adjustment, it)
            }
            tickets.forEach {
                transform(adjustment, it, weighedValues[it]!!)
            }
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
    override fun weigh(source: BookingAddOn, ticket: Ticket) : BigDecimal{
        return weighByApplicableGrossRevenue(source.amount, source.booking, ticket, applicability(source))
    }
    
    override fun transform(source: BookingAddOn, ticket: Ticket, weighedAmount: BigDecimal) {
        TicketBookingAddOn.new { 
            subject = ticket
            sourceAdjustment = source
            addOn = source.addOn
            amount = weighedAmount
            prompt = source.prompt
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

    override fun weigh(source: BookingManualAdjustment, ticket: Ticket) : BigDecimal{
        return weighByApplicableGrossRevenue(source.amount, source.booking, ticket, applicability(source))
    }
    
    override fun transform(source: BookingManualAdjustment, ticket: Ticket, weighedAmount: BigDecimal) {
        TicketBookingManualAdjustment.new {
            subject = ticket
            sourceAdjustment = source
            description = source.description
            amount = weighedAmount
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

    override fun weigh(source: BookingItemAddOn, ticket: Ticket) : BigDecimal{
        return weighByApplicableGrossRevenue(source.amount, source.subject, ticket, applicability(source))
    }
    
    override fun transform(source: BookingItemAddOn, ticket: Ticket, weighedAmount: BigDecimal) {
        TicketBookingItemAddOn.new {
            subject = ticket
            sourceAdjustment = source
            addOn = source.addOn
            amount = weighedAmount
            prompt = source.prompt
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


internal object BookingDiscountTransformation : TicketAdjustmentTransform<BookingDiscount>() {
    override fun prepare(ticket : Ticket) {
        TicketBookingDiscounts.deleteWhere {
            TicketBookingDiscounts.parent eq ticket.id
        }
    }
    override fun weigh(source: BookingDiscount, ticket: Ticket) : BigDecimal{
        return source.discount.basis.weightMethod(
                source.amount, 
                source.subject, 
                ticket, 
                applicability(source)
        )
    }
    
    override fun transform(source: BookingDiscount, ticket: Ticket, weighedAmount: BigDecimal) {
        TicketBookingDiscount.new {
            subject = ticket
            sourceAdjustment = source
            discount = source.discount
            amount = weighedAmount
        }
    }
    override fun applicability(source : BookingDiscount) : (Ticket) -> Boolean {
        return {
            ticket : Ticket ->
            ticket.personCategory in source.discount.personCategories
                    && ticket.bookingItem.event.product in source.discount.products
        }
    }
    override fun sources(booking : Booking) : Iterable<BookingDiscount> {
        return booking.discounts
    }
}
