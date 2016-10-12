package ticketpile.service

import com.oembedler.moon.graphql.boot.EnableGraphQLServer
import com.oembedler.moon.graphql.boot.GraphQLSchemaLocator
import com.oembedler.moon.graphql.engine.GraphQLSchemaConfig
import com.oembedler.moon.graphql.engine.GraphQLSchemaHolder
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.PropertySource
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import springfox.documentation.builders.ApiInfoBuilder
import springfox.documentation.builders.PathSelectors
import springfox.documentation.service.ApiKey
import springfox.documentation.service.AuthorizationScope
import springfox.documentation.service.SecurityReference
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spi.service.contexts.SecurityContext
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger.web.ApiKeyVehicle
import springfox.documentation.swagger.web.SecurityConfiguration
import springfox.documentation.swagger2.annotations.EnableSwagger2
import ticketpile.service.advance.AdvanceManager
import ticketpile.service.advance.bookingQueueSync
import ticketpile.service.advance.individualBookingSync
import ticketpile.service.advance.initializeSynchronization
import ticketpile.service.database.initializeModel
import ticketpile.service.graphql.TicketPileGraphQLSchema
import ticketpile.service.security.initializeSecurity
import ticketpile.service.springconfig.apiTokenHeader
import ticketpile.service.util.transaction
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

/**
 * Initialization flow:
 * 
 * - Spring app launches
 * - Kicks off job to connect to DB and create/update tables
 * - Starts Advance synchronization jobs on schedule
 */
@SpringBootApplication(exclude = arrayOf( SecurityAutoConfiguration::class ))
@EnableSwagger2
@EnableConfigurationProperties
//@EnableGraphQLServer
open class TicketPile {
    companion object {
        @JvmStatic fun main(args: Array<String>) {
            SpringApplication.run(TicketPile::class.java, *args)
        }
    }
    
    @Bean
    open fun tpApi() : Docket {
        return Docket(DocumentationType.SWAGGER_2)
                .apiInfo(ApiInfoBuilder()
                .title("TicketPile API")
                .description("TicketPile is an ETL and integration testing layer for Advance. " +
                        "It transforms JSON data from the Advance API into a normalized relational database. " +
                        "<p>" + 
                        "It logs inconsistencies it finds in data it retrieves from Advance. It will also provides errors " +
                        "when totals from imported data fail to match those on the Advance reservation.")
                .version("0.1")
                .build())
        .securitySchemes(listOf(ApiKey("mykey", apiTokenHeader, "header")))
        .securityContexts(listOf(
                SecurityContext.builder()
                .securityReferences(listOf(
                        SecurityReference("mykey", arrayOf(
                                AuthorizationScope("global", "accessEverything")
                        ))
                ))
                .forPaths(PathSelectors.regex("/anyPath.*"))
                .build()
        ))
    }
    @Bean
    open fun security() : SecurityConfiguration {
        return SecurityConfiguration(
            null,
            null,
            null,
            "TicketPile",
            null,
            ApiKeyVehicle.HEADER,
            apiTokenHeader,
            null
        )
    }

    @Bean
    //@ConditionalOnMissingBean
    //@Throws(ClassNotFoundException::class)
    open fun graphQLSchemaLocator(): GraphQLSchemaLocator {
        val graphQLSchemaHolders = mutableMapOf<String, GraphQLSchemaHolder>()
        graphQLSchemaHolders[TicketPileGraphQLSchema.queryType.name] = 
            GraphQLSchemaHolder(
                    TicketPileGraphQLSchema.queryType.name,
                    TicketPileGraphQLSchema,
                    GraphQLSchemaConfig(),
                    ConcurrentHashMap()
            )
        //val graphQLSchemaBuilder = graphQLSchemaBuilder()
        //val schemaClasses = initialSchemaClassesSet()
        /*if (schemaClasses.size > 0) {
            for (schema in schemaClasses) {
                val schemaHolder = graphQLSchemaBuilder.buildSchema(schema)
                graphQLSchemaHolders.put(schemaHolder.getSchemaName(), schemaHolder)
            }
        }*/
        return GraphQLSchemaLocator(graphQLSchemaHolders)
    }
}

@Component
open class DBConnection() : CommandLineRunner, Ordered {
    @Autowired
    var config = DBConfig()
    
    override fun run(vararg args : String) {
        println("Establishing TicketPile Database Connection...")
        println("DB Server: ${config.url}")
        println("DB User: ${config.user}")
        
        Database.connect(getDataSource())
        println("Setting up database tables")
        transaction( statement = {
            initializeModel()
            initializeSynchronization()
            initializeSecurity()
        }, repetitionAttempts = 0)
    }
    
    private fun getDataSource() : DataSource {
        val hikariConfig = HikariConfig()
        hikariConfig.jdbcUrl = config.url
        hikariConfig.username = config.user
        hikariConfig.password = config.password
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true")
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250")
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")

        return HikariDataSource(hikariConfig)
    }
    
    override fun getOrder(): Int {
        return 1
    }
}

@Component
@PropertySource("classpath:db.properties")
@ConfigurationProperties(prefix = "db")
open class DBConfig() {
    var url = ""
    var user = ""
    var password = ""
}

fun wrapTask(task: () -> Unit, taskName: String) : () -> Unit {
    return {
        try {
            task()
        } catch(t: Throwable) {
            val sw = StringWriter()
            t.printStackTrace(PrintWriter(sw))
            println("$taskName error: $sw")
        }
    }
}

@Component
open class BackgroundJobs() : CommandLineRunner, Ordered {
    override fun run(vararg args : String) {
        val scheduler = Executors.newScheduledThreadPool(13)
        scheduler.scheduleAtFixedRate(
                wrapTask(bookingQueueSync, "Booking Queue Sync"),
                0, AdvanceManager.syncPeriodSeconds, TimeUnit.SECONDS
        )
        // Schedule multiple tasks for booking sync
        for(offset in 1..3) {
            scheduler.scheduleAtFixedRate(
                    wrapTask(individualBookingSync, "Individual Booking Sync"),
                    0, 10, TimeUnit.MILLISECONDS
            )
        }
    }

    override fun getOrder(): Int {
        return 2
    }
}