package ticketpile.service.util

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializationConfig
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.sql.transactions.TransactionManager

/**
 * Exposed requires all access to be done in a transaction, including reads
 * that will happen during serialization.  This serializer will automatically
 * use the minimum safe level of isolation so we don't have to think about it.
 * 
 * Effectively, this means the response will always be read in a separate transaction
 * from the request.
 * 
 * Created by jonlatane on 8/29/16.
 */
private class TransactionSerializer(val serializer : JsonSerializer<Any>) : JsonSerializer<Any>() {
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
@Suppress("UNCHECKED_CAST")
class TransactionSerializerModifier : BeanSerializerModifier() {
    override fun modifySerializer(
            serializationConfig: SerializationConfig,
            beanDescription: BeanDescription,
            jsonSerializer: JsonSerializer<*>): JsonSerializer<Any> {
        return TransactionSerializer(jsonSerializer as JsonSerializer<Any>)
    }
}