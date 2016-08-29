package ticketpile.service.database

import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.Table

/**
 * The base ways we can affect the price of Tickets, Events
 * and Bookings.  To be used in the database schema.
 * Created by jonlatane on 8/28/16.
 */


open class RelatableTable(val singularName : String) : IntIdTable() {
}

// To be used for things that don't affect price
open class ReferenceTable(subject : RelatableTable) : IntIdTable() {
    val parent = reference(subject.singularName, subject)
}

open class AdjustmentTable(subject : RelatableTable) : ReferenceTable(subject) {
    val amount = decimal("amount", precision = 16, scale = 16)
}

open class DiscountTable(subject : RelatableTable) : AdjustmentTable(subject) {
    val discount = reference("discount", Discounts)
}

open class ManualAdjustmentTable(subject : RelatableTable) : AdjustmentTable(subject) {
    val description = varchar("description", length = 512)
}

open class AddOnTable(subject : RelatableTable) : AdjustmentTable(subject) {
    val addon = reference("addon", AddOns)
}