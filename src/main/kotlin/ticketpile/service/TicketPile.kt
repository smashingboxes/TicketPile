package ticketpile.service

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import springfox.documentation.swagger2.annotations.EnableSwagger2
import ticketpile.service.database.*

@SpringBootApplication
@EnableSwagger2
open class TicketPile {
    companion object {
        @JvmStatic fun main(args: Array<String>) {
            //H2 gets weird with how Clob datatypes are read by the Exposed library
            //Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=MySQL", driver = "org.h2.Driver")
            Database.connect("jdbc:mysql://127.0.0.1/ticketpile?socketTimeout=60000", 
                    driver = "com.mysql.jdbc.Driver",
                    user = "root", 
                    password = "password"
            )
            transaction {
                logger.addLogger(StdOutSqlLogger())
                SchemaUtils.createMissingTablesAndColumns(
                        Bookings,
                        Events,
                        Tickets,
                        PersonCategories,
                        Products,
                        AddOns,
                        Discounts,
                        BookingAddOns,
                        BookingDiscounts,
                        BookingManualAdjustments,
                        TicketAddOns,
                        TicketDiscounts,
                        TicketManualAdjustments,
                        EventAddOns
                )
            }
            SpringApplication.run(TicketPile::class.java, *args)
        }
    }
}

@ResponseStatus(value = HttpStatus.NOT_FOUND)
class ResourceNotFoundException : RuntimeException() {

}