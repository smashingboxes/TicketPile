package ticketpile.service.model.transformation

import ticketpile.service.model.Ticket
import java.math.BigDecimal

/**
 * Basis of, like, everything.  By implementing this interface, you 
 * assert that none of the below fields will ever return null.
 * 
 * @see [RelationalEntity.cacheColumn]
 * Created by jonlatane on 10/15/16.
 */
interface Weighable {
    val tickets : List<Ticket>
    val basePrice : BigDecimal?
    val discountsAmount : BigDecimal?
    val feesAmount : BigDecimal?
    val addOnsAmount : BigDecimal?
    val manualAdjustmentsAmount : BigDecimal?
    val itemAddOnsAmount : BigDecimal?
    val grossAmount : BigDecimal?
    val totalAmount : BigDecimal?
}