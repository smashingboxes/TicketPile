package ticketpile.service.database

import ticketpile.service.util.ReferenceTable
import ticketpile.service.util.RelationalTable

/**
 * The base ways we can affect the price of Tickets, Events
 * and Bookings.  To be used in the database schema.
 * Created by jonlatane on 8/28/16.
 */

open class AdjustmentTable(singularName: String, subject : RelationalTable) : ReferenceTable(singularName, subject) {
    val amount = decimal("amount")
}

open class DiscountTable(singularName: String, subject : RelationalTable) : AdjustmentTable(singularName, subject) {
    val discount = reference(Discounts)
}

open class ManualAdjustmentTable(singularName: String, subject : RelationalTable) : AdjustmentTable(singularName, subject) {
    val description = varchar("description", length = 512)
}

open class AddOnTable(singularName: String, subject : RelationalTable) : AdjustmentTable(singularName, subject) {
    val addon = reference(AddOns)
    val selection = varchar("selection", length = 512)
}

open class FeeTable(singularName: String, subject : RelationalTable) : AdjustmentTable(singularName, subject) {
    val description = varchar("description", length = 512)
}