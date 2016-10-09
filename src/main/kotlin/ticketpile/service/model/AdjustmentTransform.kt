package ticketpile.service.model

import org.jetbrains.exposed.sql.deleteWhere
import ticketpile.service.advance.AdvanceSyncError
import ticketpile.service.advance.SyncErrorType
import ticketpile.service.database.TicketBookingAddOns
import ticketpile.service.database.TicketBookingDiscounts
import ticketpile.service.database.TicketBookingManualAdjustments
import ticketpile.service.database.decimalScale
import ticketpile.service.util.RelationalEntity
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
        BookingItemAddOnTransformation,
        BookingFeeTransformation
)
interface Weighable {
    val tickets : List<Ticket>
    val grossRevenue : BigDecimal get() {
        var result = BigDecimal.ZERO
        tickets.forEach {
            result += it.grossRevenue
        }
        return result
    }
}

/**
 * Returns true if the {@param ticket} is applicable or if no tickets in the {@param weighable} are applicable.
 */
private fun isReallyApplicable(ticket: Ticket, weighable: Weighable, applicable: (Ticket) -> Boolean) : Boolean {
    return applicable(ticket) || weighable.tickets.filter(applicable).isEmpty()
}
/**
 * Returns a list of all applicable tickets in the weighable, or a list of all tickets in the weighable if no
 * tickets are applicable.
 */
private fun applicableTickets(weighable: Weighable, applicable: (Ticket) -> Boolean) : List<Ticket> {
    var result= weighable.tickets.filter(applicable)
    if(result.isEmpty()) {
        result = weighable.tickets
    }
    return result
}
val weighByApplicableTicketCount = {
    amount : BigDecimal, weighable: Weighable, ticket: Ticket, applicable: (Ticket) -> Boolean ->
    val applicableTickets = applicableTickets(weighable, applicable)
    if(isReallyApplicable(ticket, weighable, applicable))
        amount.setScale(decimalScale) / BigDecimal(applicableTickets.count()).setScale(decimalScale)
    else
        BigDecimal.ZERO.setScale(decimalScale)
}
val weighByApplicableGrossRevenue = {
    amount : BigDecimal, weighable: Weighable, ticket: Ticket, applicable: (Ticket) -> Boolean ->
    val applicableTickets = applicableTickets(weighable, applicable)
    if(isReallyApplicable(ticket, weighable, applicable)) {
        val applicableGross = applicableTickets.map{it.grossRevenue}.reduce { 
            amount1, amount2 ->  
            amount1 + amount2
        }
        if(applicableGross == BigDecimal.ZERO.setScale(applicableGross.scale()))
            weighByApplicableTicketCount(amount, weighable, ticket, applicable)
        else 
            amount.setScale(decimalScale) *
                    ticket.grossRevenue.setScale(decimalScale) /
                    applicableGross.setScale(decimalScale)
    } else
        BigDecimal.ZERO.setScale(decimalScale)
}

/**
 * Base class for transforming [Booking] and [BookingItem] level adjustments to
 * [MappedAdjustment]s.
 */
abstract class TicketAdjustmentTransform<Adj : Adjustment<*>>() {
    /**
     * Delete all adjustments of the managed type from the ticket
     */
    abstract fun prepare(ticket : Ticket)

    /**
     * Determine the actual amount of an adjustment on a given ticket, assuming
     * the adjustment has not yet been assessed on any tickets.
     */
    abstract fun weigh(source : Adj, ticket : Ticket) : BigDecimal

    /**
     * Apply the source adjustment to the given ticket, given the result of 
     * [weigh] as [weighedAmount].
     */
    abstract fun transform(source : Adj, ticket: Ticket, weighedAmount: BigDecimal)

    /**
     * Provide a list of all 
     */
    abstract fun sources(booking : Booking) : Iterable<Adj>

    /**
     * Determine whether this adjustment should affect the price of a ticket.  Assumed to evaluate false if
     * [mentionability] does so.  Booking Discounts are mentionable when not applicable, Booking Item AddOns
     * are not.
     */
    open fun applicability(source : Adj) : (Ticket) -> Boolean {
        return {
            ticket : Ticket ->
            true
        }
    }

    /**
     * Determine whether this adjustment should affect (i.e. have it [MappedAdjustment] even listed) for a given
     * [Ticket].
     */
    open fun mentionability(source : Adj) : (Ticket) -> Boolean {
        return {
            ticket : Ticket ->
            true
        }
    }
    
    private fun transform(booking : Booking) {
        sources(booking).forEach {
            adjustment ->
            if(booking.tickets.filter(mentionability(adjustment)).filter(applicability(adjustment)).isEmpty()) {
                AdvanceSyncError.new {
                    errorType = SyncErrorType.inApplicableAdjustment
                    this.booking = booking
                    message = "No tickets applied to ${adjustment.javaClass.simpleName} with ID " +
                            if(adjustment is RelationalEntity) adjustment.id.value.toString() else "unknown"
                }
            }
            val tickets = booking.tickets.filter(mentionability(adjustment))
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
            selection = source.selection
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


internal object BookingFeeTransformation : TicketAdjustmentTransform<BookingFee>() {
    override fun prepare(ticket : Ticket) {
        TicketBookingManualAdjustments.deleteWhere {
            TicketBookingManualAdjustments.parent eq ticket.id
        }
    }

    override fun weigh(source: BookingFee, ticket: Ticket) : BigDecimal{
        return weighByApplicableTicketCount(source.amount, source.booking, ticket, applicability(source))
    }

    override fun transform(source: BookingFee, ticket: Ticket, weighedAmount: BigDecimal) {
        TicketBookingFee.new {
            subject = ticket
            sourceAdjustment = source
            description = source.description
            amount = weighedAmount
        }
    }
    override fun sources(booking : Booking) : Iterable<BookingFee> {
        return booking.fees
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
            selection = source.selection
        }
    }
    override fun applicability(source : BookingItemAddOn) : (Ticket) -> Boolean {
        return {
            ticket : Ticket ->
            ticket.bookingItem == source.subject
        }
    }
    override fun mentionability(source : BookingItemAddOn) : (Ticket) -> Boolean {
        return {
            ticket : Ticket ->
            ticket.bookingItem == source.subject
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
