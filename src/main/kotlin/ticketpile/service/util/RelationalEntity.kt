package ticketpile.service.util

import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import ticketpile.service.database.RelationalTable
import kotlin.reflect.KProperty

/**
 * Uses ReferenceTable to
 * Created by jonlatane on 8/30/16.
 */
abstract class RelationalEntity(id: EntityID<Int>) : IntEntity(id) {
    val PK = IDDelegate(this)

    class IDDelegate(val entity: Entity<Int>) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): Int {
            return entity.id.value
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {

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

abstract class PrimaryEntity(id: EntityID<Int>, table: RelationalTable) : RelationalEntity(id) {
    internal val singularName = table.singularName
    @get:JsonProperty
    var externalSource by table.externalSource
    @get:JsonProperty
    var externalId by table.externalId
}