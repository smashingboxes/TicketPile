package ticketpile.service

import AdvanceSyncTask
import initializeSynchronization
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.StdOutSqlLogger
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
import ticketpile.service.advance.AdvanceLocationManager
import ticketpile.service.database.initializeDB
import ticketpile.service.security.initializeSecurity
import ticketpile.service.util.transaction

@SpringBootApplication(exclude = arrayOf( SecurityAutoConfiguration::class ))
@EnableSwagger2
@EnableConfigurationProperties
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
                .description("TicketPile is an ETL layer that transforms " +
                        "JSON data from the Advance API into a normalized relational DB " +
                        "format that can be easily imported into Sisense " +
                        "or other BI tools.")
                .version("0.1")
                .build())
        .securitySchemes(listOf(ApiKey("mykey", "Bearer", "header")))
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
            null,
            null,
            ApiKeyVehicle.HEADER,
            "Bearer",
            null
        )
    }
}


@Component
open class DBConnection() : CommandLineRunner, Ordered {
    @Autowired
    var config = DBConfig()
    
    override fun run(vararg args : String) {
        println("Establishing TicketPile Database Connection...")
        println("DB Server: ${config.url}")
        println("DB Driver: ${config.driver}")
        println("DB User: ${config.user}")
        Database.connect(
                url=config.url,
                driver=config.driver,
                user=config.user,
                password=config.password
        )
        println("Setting up database tables")
        transaction {
            logger.addLogger(StdOutSqlLogger())
            initializeDB()
            initializeSynchronization()
            initializeSecurity()
        }
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
    var driver = ""
    var user = ""
    var password = ""
}


@Component
open class AdvanceSynchronizationJob() : CommandLineRunner, Ordered {

    override fun run(vararg args : String) {
        println("Starting Advance synchronization job")
        while(true) {
            transaction {
                //try {
                    val task = AdvanceSyncTask.all().firstOrNull()
                    if (task != null) {
                        if (task.bookingQueue.isEmpty()) {
                            task.delete()
                            return@transaction
                        }
                        //There is at least one booking to be processed.
                        val reservation = task.bookingQueue.firstOrNull()
                        if (reservation != null) {
                            println("Advance sync: Importing booking ${reservation.reservationId} from ${task.advanceHost}")
                            val manager = AdvanceLocationManager(task.advanceHost, task.advanceAuthKey, task.advanceLocationId)
                            val advanceReservation = manager.getAdvanceBooking(reservation.reservationId)
                            manager.importByAdvanceReservation(advanceReservation)
                            reservation.delete()
                        }
                    }
                //} catch (throwable : Throwable) {
                 //   println("Error in Advance Synchronization job: $throwable")
                //}
            }
            //Thread.sleep(100)
        }
    }

    override fun getOrder(): Int {
        return 2
    }
}