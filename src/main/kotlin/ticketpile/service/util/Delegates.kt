package ticketpile.service.util

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import kotlin.reflect.KProperty

/**
 * Created by jonlatane on 8/30/16.
 */

class IDDelegateOn(val entity : Entity<Int>) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Int {
        return entity.id.value
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {

    }
}

class Children<Type : IntEntity>(val parentObject: IntEntity, val childClass: IntEntityClass<Type>, val childColumn: Column<EntityID<Int>>) {
    operator fun getValue(thisRef: IntEntity, property: KProperty<*>): Iterable<Type> {
        return childClass.find { childColumn eq parentObject.id }.map {it -> it}
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {

    }
}