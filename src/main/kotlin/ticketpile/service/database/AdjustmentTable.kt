package ticketpile.service.database

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable

/**
 * The base ways we can affect the price of Tickets, Events
 * and Bookings.  To be used in the database schema.
 * Created by jonlatane on 8/28/16.
 */

private class RelationalEntityClass<EntityType : IntEntity>(table: IntIdTable) : IntEntityClass<EntityType>(table)

open class RelationalTable(val singularName : String) : IntIdTable() {
    var externalSource = varchar(name="externalHost", length = 128).nullable().index()
    var externalId = integer("externalId").nullable().index()
    
    fun <EntityType : IntEntity> companion() : IntEntityClass<EntityType> = RelationalEntityClass(this)
}

// To be used for things that don't affect price
open class ReferenceTable(singularName: String, subject : RelationalTable) : RelationalTable(singularName) {
    val parent = reference(subject.singularName, subject)
}

open class AdjustmentTable(singularName: String, subject : RelationalTable) : ReferenceTable(singularName, subject) {
    val amount = decimal("amount", precision = 65, scale = 30)
}

open class DiscountTable(singularName: String, subject : RelationalTable) : AdjustmentTable(singularName, subject) {
    val discount = reference("discount", Discounts)
}

open class ManualAdjustmentTable(singularName: String, subject : RelationalTable) : AdjustmentTable(singularName, subject) {
    val description = varchar("description", length = 512)
}

open class AddOnTable(singularName: String, subject : RelationalTable) : AdjustmentTable(singularName, subject) {
    val addon = reference("addon", AddOns)
}