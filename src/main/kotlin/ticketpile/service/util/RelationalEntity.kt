package ticketpile.service.util

import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.sql.Column
import ticketpile.service.database.ReferenceTable
import ticketpile.service.database.RelationalTable
import kotlin.reflect.KProperty

/**
 * Uses ReferenceTable to
 * Created by jonlatane on 8/30/16.
 */
abstract class RelationalEntity(id: EntityID<Int>) : IntEntity(id) {
    val PK = IDDelegate(this)
    
    fun <ChildType : IntEntity> children(
            childClass: IntEntityClass<ChildType>
    ): Children<ChildType> {
        return Children(this, childClass, (childClass.table as ReferenceTable).parent)
    }

    fun <ChildType : IntEntity> children(
            childClass: IntEntityClass<ChildType>,
            childColumn: Column<EntityID<Int>>
    ): Children<ChildType> {
        return Children(this, childClass, childColumn)
    }

    class IDDelegate(val entity: Entity<Int>) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): Int {
            return entity.id.value
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {

        }
    }

    class Children<Type : IntEntity>(val parentObject: IntEntity, val childClass: IntEntityClass<Type>, val childColumn: Column<EntityID<Int>>) {
        operator fun getValue(thisRef: IntEntity, property: KProperty<*>): Iterable<Type> {
            return childClass.find { childColumn eq parentObject.id }.map { it -> it }
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {

        }
    }
}

abstract class PrimaryEntity(id: EntityID<Int>, table: RelationalTable) : RelationalEntity(id) {
    internal val singularName = table.singularName
    @get:JsonProperty
    var externalSource by table.externalSource
    @get:JsonProperty
    var externalId by table.externalId
}