package ticketpile.service.util

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializationConfig
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier
import org.jetbrains.exposed.dao.Entity
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
            transaction(statement = {
                serializer.serialize(o, jsonGenerator, serializerProvider)
            }, logging = false)
        } else if(o is Iterable<*>) {
            serializer.serialize(o.map{it}, jsonGenerator, serializerProvider)
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

var defaultIsolationLevel = Connection.TRANSACTION_REPEATABLE_READ
fun <T> transaction(
        logging: Boolean = true,
        isolationLevel: Int = defaultIsolationLevel,
        repetitionAttempts: Int = 3,
        statement: Transaction.() -> T
): T = org.jetbrains.exposed.sql.transactions.transaction(
        isolationLevel,
        repetitionAttempts,
        {
            if (logging)
                logger.addLogger(StdOutSqlLogger())
            statement()
        })
