package ticketpile.service.util

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.Column
import java.math.BigDecimal

/**
 * Created by jonlatane on 10/9/16.
 */
val decimalScale = 30
val decimalPrecision = 65
val BigZero = BigDecimal.ZERO.setScale(decimalScale)
val BigOne = BigDecimal.ONE.setScale(decimalScale)

open class RelationalTable(val singularName : String) : IntIdTable() {
    var externalSource = varchar(name="externalHost", length = 128).nullable().index()
    var externalId = integer("externalId").nullable().index()
    fun reference(foreign: RelationalTable) : Column<EntityID<Int>> {
        return reference(foreign.singularName, foreign)
    }
    fun decimal(name: String) : Column<BigDecimal> {
        return decimal(name, precision = decimalPrecision, scale = decimalScale)
    }
}

// To be used for things that don't affect price
open class ReferenceTable(singularName: String, subject : RelationalTable) : RelationalTable(singularName) {
    val parent = reference(subject.singularName, subject)
}