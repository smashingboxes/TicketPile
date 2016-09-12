package ticketpile.service

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration
import org.springframework.context.annotation.Bean
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
import ticketpile.service.database.initializeDB
import ticketpile.service.security.initializeSecurity
import ticketpile.service.util.transaction

@SpringBootApplication(exclude = arrayOf( SecurityAutoConfiguration::class ))
@EnableSwagger2
open class TicketPile {
    companion object {
        @JvmStatic fun main(args: Array<String>) {
            //H2 gets weird with how Clob datatypes are read by the Exposed library
            //Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=MySQL", driver = "org.h2.Driver")
            /*Database.connect(
                    url="jdbc:sqlite::memory:",
                    driver = "org.sqlite.JDBC"
            )
            defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE*/
            Database.connect(
                    url="jdbc:mysql://127.0.0.1/ticketpile?socketTimeout=60000", 
                    driver="com.mysql.jdbc.Driver",
                    user="root", 
                    password="password"
            )
            transaction {
                logger.addLogger(StdOutSqlLogger())
                initializeDB()
                initializeSecurity()
            }
            SpringApplication.run(TicketPile::class.java, *args)
        }
    }
    
    @Bean
    open fun tpApi() : Docket {
        return Docket(DocumentationType.SWAGGER_2)
                .apiInfo(ApiInfoBuilder()
                .title("TicketPile API")
                .description("TicketPile is an ETL layer that transforms " +
                        "JSON data from the Advance API into a normalized relational DB" +
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
/*
@Configuration
@PropertySource("classpath:/db.properties")
open class DBConfig() {
    @Autowired
    var data : Environment? = null

    @Bean
    open fun connect() {
        val env = data!!
        Database.connect(
                url=env.getProperty("db.url"),
                driver = env.getProperty("db.driver"),
                user = env.getProperty("db.user"),
                password = env.getProperty("db.password")
        )
    }
    
    companion object {
        @JvmStatic
        @Bean
        open fun placeHolderConfigurer(): PropertySourcesPlaceholderConfigurer {
            return PropertySourcesPlaceholderConfigurer()
        }
    }
}*/