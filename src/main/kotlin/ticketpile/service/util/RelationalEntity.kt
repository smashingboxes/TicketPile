package ticketpile.service.util

import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.sql.Column
import kotlin.reflect.KProperty

/**
 * Base classes for application entity model.
 * 
 * Created by jonlatane on 8/30/16.
 */
abstract class RelationalEntity(id: EntityID<Int>) : IntEntity(id) {
    val PK = IDDelegate(this)

    class IDDelegate(val entity: Entity<Int>) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): Int {
            return entity.id.value
        }
    }
    
    override fun hashCode() : Int {
        return id.value
    }
    
    override fun equals(other : Any?) : Boolean {
        return other?.javaClass == javaClass
            && (other as RelationalEntity).PK == PK
    }
}

class Children<in Parent:Entity<Int>, out Child:Entity<Int>>
(val referrers : Referrers<Int, Parent, Int, Child>) {
    operator fun getValue(o: Parent, desc: KProperty<*>): List<Child> {
        return referrers.getValue(o, desc).toList()
    }
}

open class RelationalEntityClass<T : RelationalEntity>(table: IdTable<Int>) : IntEntityClass<T>(table) {
    infix fun childrenOn(column: Column<EntityID<Int>>) = Children(super.referrersOn(column, true))
}

abstract class PrimaryEntity(id: EntityID<Int>, table: RelationalTable) : RelationalEntity(id) {
    internal val singularName = table.singularName
    @get:JsonProperty
    var externalSource by table.externalSource
    @get:JsonProperty
    var externalId by table.externalId
}