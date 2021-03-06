package ticketpile.service.util

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configures the Jackson [ObjectMapper] to not auto-detect anything
 * for serialization (forcing the use of [JsonProperty] annotations).
 * 
 * Also configures Joda, Kotlin and [TransactionSerializerModifier] components.
 * 
 * Created by jonlatane on 8/29/16.
 */
@Configuration
open class JacksonConfiguration {

    @Bean
    open fun objectMapper(): ObjectMapper {
        val mapper = ObjectMapper()
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        mapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, true)
        mapper.disable(MapperFeature.AUTO_DETECT_CREATORS,
                MapperFeature.AUTO_DETECT_FIELDS,
                MapperFeature.AUTO_DETECT_GETTERS,
                MapperFeature.AUTO_DETECT_IS_GETTERS)

        val module = KotlinModule()
        module.setSerializerModifier(TransactionSerializerModifier())
        mapper.registerModule(module)
        
        mapper.registerModule(JodaModule())
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)

        return mapper
    }
}