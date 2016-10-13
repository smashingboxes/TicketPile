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
    
    class WrappedColumn<T>(
            val getter: (Entity<*>, KProperty<*>) -> T, 
            val setter : (Entity<*>, KProperty<*>, T) -> Unit
    ) {
        operator fun getValue(o: Entity<*>, desc: KProperty<*>): T {
            return getter(o, desc)
        }
        operator fun setValue(o: Entity<*>, desc: KProperty<*>, value: T) {
            setter(o, desc, value)
        }
    }

    /**
     * If actual value of input [column] is null, returns a delegate that will
     * calculate the value, set it and return it.  *Any time the value is manually modified,
     * the delegate will set the value to null for recalculation*
     */
    fun <T> cacheColumn(column : Column<T?>, calculation : () -> T) : WrappedColumn<T?> {
        return WrappedColumn(
             getter = {
                 o, desc ->
                 var result = column.getValue(o, desc)
                 if(result == null) {
                     result = calculation()
                     column.setValue(o, desc, result)
                 }
                 result
             },
             setter = {
                 o, desc, value ->
                 column.setValue(o, desc, null)
             }
        )
    }
    
    fun <T> notifierColumn(column : Column<T>, notifier : () -> Unit) : WrappedColumn<T> {
        return WrappedColumn(
                getter = {
                    o, desc ->
                    column.getValue(o, desc)
                },
                setter = {
                    o, desc, value : T ->
                    column.setValue(o, desc, value)
                    notifier()
                }
        )
    }

    fun <T> cacheNotifierColumn(column : Column<T?>, calculation : () -> T, notifier : () -> Unit) : WrappedColumn<T?> {
        return WrappedColumn<T?>(
                getter = {
                    o, desc ->
                    var result = column.getValue(o, desc)
                    if(result == null) {
                        result = calculation()
                        column.setValue(o, desc, result)
                        notifier()
                    }
                    result
                },
                setter = {
                    o, desc, value : T? ->
                    column.setValue(o, desc, null)
                    notifier()
                }
        )
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