package ticketpile.service.database

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.Column

/**
 * The base ways we can affect the price of Tickets, Events
 * and Bookings.  To be used in the database schema.
 * Created by jonlatane on 8/28/16.
 */

open class RelationalTable(val singularName : String) : IntIdTable() {
    var externalSource = varchar(name="externalHost", length = 128).nullable().index()
    var externalId = integer("externalId").nullable().index()
    fun reference(foreign: RelationalTable) : Column<EntityID<Int>> {
        return reference(foreign.singularName, foreign)
    }
}

// To be used for things that don't affect price
open class ReferenceTable(singularName: String, subject : RelationalTable) : RelationalTable(singularName) {
    val parent = reference(subject.singularName, subject)
}

open class AdjustmentTable(singularName: String, subject : RelationalTable) : ReferenceTable(singularName, subject) {
    val amount = decimal("amount", precision = 65, scale = 30)
}

open class DiscountTable(singularName: String, subject : RelationalTable) : AdjustmentTable(singularName, subject) {
    val discount = reference(Discounts)
}

open class ManualAdjustmentTable(singularName: String, subject : RelationalTable) : AdjustmentTable(singularName, subject) {
    val description = varchar("description", length = 512)
}

open class AddOnTable(singularName: String, subject : RelationalTable) : AdjustmentTable(singularName, subject) {
    val addon = reference(AddOns)
    val prompt = varchar("prompt", length = 512)
}