package ticketpile.service.model.basis

import ticketpile.service.model.Ticket
import ticketpile.service.model.transformation.Weighable
import ticketpile.service.util.BigZero
import ticketpile.service.util.decimalScale
import java.math.BigDecimal

/**
 * Created by jonlatane on 10/15/16.
 */
val weighByApplicableTicketCount = {
    amount : BigDecimal, weighable: Weighable, ticket: Ticket, applicable: (Ticket) -> Boolean ->
    val applicableTickets = applicableTickets(weighable, applicable)
    if(isReallyApplicable(ticket, weighable, applicable))
        amount.setScale(decimalScale) / BigDecimal(applicableTickets.count()).setScale(decimalScale)
    else
        BigZero
}
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