package ticketpile.service

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
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
import ticketpile.service.advance.bookingQueueSync
import ticketpile.service.advance.individualBookingSync
import ticketpile.service.database.initializeDB
import ticketpile.service.security.initializeSecurity
import ticketpile.service.util.transaction
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

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
        /*Database.connect(
                url=config.url,
                driver=config.driver,
                user=config.user,
                password=config.password
        )*/
        Database.connect(getDataSource())
        println("Setting up database tables")
        transaction {
            logger.addLogger(StdOutSqlLogger())
            initializeDB()
            initializeSynchronization()
            initializeSecurity()
        }
    }
    
    private fun getDataSource() :DataSource {
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
    var driver = ""
    var user = ""
    var password = ""
}


@Component
open class BackgroundJobs() : CommandLineRunner, Ordered {
    override fun run(vararg args : String) {
        val scheduler = Executors.newScheduledThreadPool(13)
        scheduler.scheduleAtFixedRate(
                bookingQueueSync, 0, 5, TimeUnit.SECONDS
        )
        scheduler.scheduleAtFixedRate(
                individualBookingSync, 0, 500, TimeUnit.MILLISECONDS
        )
    }

    override fun getOrder(): Int {
        return 2
    }
}