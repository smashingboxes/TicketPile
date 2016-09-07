package ticketpile.service

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import springfox.documentation.swagger2.annotations.EnableSwagger2
import ticketpile.service.database.initializeDB

@SpringBootApplication
@EnableSwagger2
open class TicketPile {
    companion object {
        @JvmStatic fun main(args: Array<String>) {
            //H2 gets weird with how Clob datatypes are read by the Exposed library
            //Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=MySQL", driver = "org.h2.Driver")
            /*Database.connect(
                    url="jdbc:sqlite:memory",
                    driver = "org.sqlite.JDBC"
            )*/
            Database.connect(
                    url="jdbc:mysql://127.0.0.1/ticketpile?socketTimeout=60000", 
                    driver = "com.mysql.jdbc.Driver",
                    user = "root", 
                    password = "password"
            )
            transaction {
                logger.addLogger(StdOutSqlLogger())
                initializeDB()
            }
            SpringApplication.run(TicketPile::class.java, *args)
        }
    }
}