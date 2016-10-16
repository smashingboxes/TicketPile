package ticketpile.service.model.basis

import ticketpile.service.model.transformation.Weighable
import ticketpile.service.util.decimalScale
import java.math.BigDecimal

/**
 * Methods for applying AddOns to bookings as they are found from
 * Advance.
 * 
 * Created by jonlatane on 10/7/16.
 */
val pricePerItem = {
    amount: BigDecimal, weighable: Weighable ->
    amount
}
val pricePerPerson = {
    amount: BigDecimal, weighable: Weighable ->
    val result = (amount.setScale(decimalScale)) *
            (BigDecimal(weighable.tickets.count()))
    result
}