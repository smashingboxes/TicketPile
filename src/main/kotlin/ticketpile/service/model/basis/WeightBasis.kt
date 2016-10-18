package ticketpile.service.model.basis

import ticketpile.service.model.Ticket
import ticketpile.service.model.transformation.Weighable
import ticketpile.service.util.BigZero
import ticketpile.service.util.decimalScale
import java.math.BigDecimal

/**
 * Methods used to project values of [Adjustments] on [Booking] and [BookingItem]s
 * onto [Ticket]s
 * Created by jonlatane on 10/15/16.
 */

/**
 * Weighs the initial value onto every ticket with the same value.
 * 
 * Example: $10 adjustment, 5 tickets => $2 adjustment per ticket
 */
val weighByApplicableTicketCount = {
    amount : BigDecimal, weighable: Weighable, ticket: Ticket, applicable: (Ticket) -> Boolean ->
    val applicableTickets = applicableTickets(weighable, applicable)
    if(isReallyApplicable(ticket, weighable, applicable))
        amount.setScale(decimalScale) / BigDecimal(applicableTickets.count()).setScale(decimalScale)
    else
        BigZero
}

/**
 * Weighs the initial value onto every ticket based on its calculated gross.
 * 
 * Example: $5 adjustment, 1 $10 ticket, 1 $15 ticket 
 *          => $2 adjustment on $10 ticket, $3 adjustment on $15 ticket
 */
val weighByApplicableGrossRevenue = {
    amount : BigDecimal, weighable: Weighable, ticket: Ticket, applicable: (Ticket) -> Boolean ->
    val applicableTickets = applicableTickets(weighable, applicable)
    if(isReallyApplicable(ticket, weighable, applicable)) {
        val applicableGross = applicableTickets.map { it.grossAmount }.fold(
                initial = BigZero,
                operation = {
                    amount1, amount2 ->
                    amount1!! + amount2!!
                })
        if(applicableGross == BigZero)
            weighByApplicableTicketCount(amount, weighable, ticket, applicable)
        else 
            amount.setScale(decimalScale) *
                    ticket.grossAmount!!.setScale(decimalScale) /
                    applicableGross.setScale(decimalScale)
    } else
        BigZero
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