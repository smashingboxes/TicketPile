package ticketpile.service.util

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializationConfig
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.sql.Connection

/**
 * Exposed requires all access to be done in a transaction, including reads
 * that will happen during serialization.  This serializer will automatically
 * use the minimum safe level of isolation so we don't have to think about it.
 * 
 * Created by jonlatane on 8/29/16.
 */
class TransactionSerializer(val serializer : JsonSerializer<Any>) : JsonSerializer<Any>() {
    
    override fun serialize(
            o : Any,
            jsonGenerator : JsonGenerator,
            serializerProvider : SerializerProvider){
        if(o is Entity<*> && TransactionManager.currentOrNull() == null) {
            transaction {
                logger.addLogger(StdOutSqlLogger())
                serializer.serialize(o, jsonGenerator, serializerProvider)
            }
        } else {
            serializer.serialize(o, jsonGenerator, serializerProvider)
        }
    }
}

class TransactionSerializerModifier : BeanSerializerModifier() {
    override fun modifySerializer(
            serializationConfig: SerializationConfig,
            beanDescription: BeanDescription,
            jsonSerializer: JsonSerializer<*>): JsonSerializer<Any> {
        return TransactionSerializer(jsonSerializer as JsonSerializer<Any>)
    }
}

class EntityIDSerializer() : StdSerializer<EntityID<*>>(null as Class<EntityID<*>>?) {
    override fun serialize(value: EntityID<*>?, gen: JsonGenerator?, provider: SerializerProvider?) {
        if(value?.value is Int)
            gen?.writeNumber(value?.value as Int)
        else
            gen?.writeString(value?.value.toString())
    }
}

var defaultIsolationLevel = Connection.TRANSACTION_REPEATABLE_READ
fun <T> transaction(statement: Transaction.() -> T): T = org.jetbrains.exposed.sql.transactions.transaction(defaultIsolationLevel, 1, statement)
