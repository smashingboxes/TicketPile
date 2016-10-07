package ticketpile.service.advance

import org.jetbrains.exposed.sql.and
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.HttpClientErrorException
import ticketpile.service.database.Bookings
import ticketpile.service.model.Booking
import ticketpile.service.util.BadRequestException
import ticketpile.service.util.transaction
import java.sql.Connection

/**
 * Controller capable of receiving Advance reservations and dumping
 * their data into TicketPile.
 * Created by jonlatane on 8/27/16.
 */
@RestController
@RequestMapping(value = "/advance")
open class AdvanceSyncController {
    @PostMapping(value = "/importBooking")
    fun importBooking(
            @RequestParam(value = "advanceHost", required = true)
            host: String,
            @RequestParam(value = "advanceUser", required = false)
            advanceUser: String?,
            @RequestParam(value = "advancePassword", required = false)
            advancePassword: String?,
            @RequestParam(value = "advanceLocationId", required = true)
            locationId: Int,
            @RequestParam(value = "advanceReservationId", required = true)
            reservationId: Int
    ) : Booking {
        val authKey: String
        // First ensure any supplied user/password was correct; discard any given auth key.
        if(advanceUser != null && advancePassword != null) {
            try {
                authKey = AdvanceLocationManager.getAuthKey(host, advanceUser, advancePassword)
            } catch(e : HttpClientErrorException) {
                throw BadRequestException("Unable to authenticate with user/password", e)
            }
        } else {
            throw BadRequestException("Either a valid auth key or a valid user/password must be provided. " +
                    "An invalid user/password is never acceptable.")
        }

        val manager = AdvanceLocationManager(host, authKey, locationId)
        val advanceReservation = manager.getAdvanceBooking(reservationId)
        transaction {
            manager.importProducts()
            manager.importPersonCategories()
            manager.importDiscountRules(advanceReservation)
            manager.importAddOns(advanceReservation)
        }
        return transaction(statement =  {
            println("Importing booking $reservationId from $host")
            val result = manager.importByAdvanceReservation(advanceReservation)
            println("Returning Spring controller result")
            result
        }, isolationLevel = Connection.TRANSACTION_SERIALIZABLE)
    }
    
    @PostMapping(value = "/synchronizeLocation")
    fun synchronizeLocation(
            @RequestParam(value = "advanceHost", required = true)
            host: String,
            @RequestParam(value = "advanceUser", required = true)
            advanceUser: String?,
            @RequestParam(value = "advancePassword", required = true)
            advancePassword: String?,
            @RequestParam(value = "advanceLocationId", required = true)
            locationId: Int
    ) : AdvanceSyncTask {
        val authKey: String
        // First ensure any supplied user/password was correct; discard any given auth key.
        if(advanceUser != null && advancePassword != null) {
            try {
                authKey = AdvanceLocationManager.getAuthKey(host, advanceUser, advancePassword)
            } catch(e : HttpClientErrorException) {
                throw BadRequestException("Unable to authenticate with user/password", e)
            }
        } else {
            throw BadRequestException("Either a valid auth key or a valid user/password must be provided. " +
                    "An invalid user/password is never acceptable.")
        }
        
        val manager = AdvanceLocationManager(host, authKey, locationId)
        val task = transaction {
            val importTask = AdvanceSyncTask.find {
                (AdvanceSyncTasks.advanceHost eq manager.source) and
                        (AdvanceSyncTasks.advanceLocationId eq locationId)
            }.firstOrNull() ?: AdvanceSyncTask.new {
                advanceHost = manager.source
                advanceAuthKey = authKey
                advanceLocationId = locationId
            }
            importTask.advanceUser = advanceUser
            importTask.advancePassword = advancePassword
            importTask.advanceAuthKey = authKey
            importTask.authenticated = true
            importTask
        }
        return task
    }
    
    @GetMapping(value = "/synchronizationQueues")
    fun synchronizationQueues() : List<AdvanceSyncTask> {
        return transaction {
            AdvanceSyncTask.all().map {it}
        }
    }

    @PostMapping(value = "/queueBooking/{advanceBookingId}")
    fun queueBooking(
            @PathVariable("advanceBookingId")
            advanceBookingId: Int,
            @RequestParam(value = "advanceHost", required = true)
            host: String,
            @RequestParam(value = "advanceLocationId", required = true)
            locationId: Int
    ) : AdvanceSyncTask {
        val source = AdvanceLocationManager.toSource(host)
        val task = transaction {
            val importTask = AdvanceSyncTask.find {
                (AdvanceSyncTasks.advanceHost eq source)
                (AdvanceSyncTasks.advanceLocationId eq locationId)
            }.firstOrNull() ?: throw BadRequestException("Synchronization is not configured " +
                    "for this location and host.")
            AdvanceSyncTaskBooking.new {
                reservationId = advanceBookingId
                task = importTask
            }
            importTask
        }
        return task
    }


    @GetMapping(
            value = "/booking/{advanceBookingId}",
            produces = arrayOf(MediaType.APPLICATION_JSON_UTF8_VALUE)
    )
    fun getBooking(
            @PathVariable("advanceBookingId")
            advanceBookingId: Int,
            @RequestParam(value = "advanceHost", required = true)
            host: String
    ) : Booking {
        val source = AdvanceLocationManager.toSource(host)
        return transaction {
            Booking.find {
                (Bookings.externalSource eq source) and 
                        (Bookings.externalId eq advanceBookingId)
            }.firstOrNull()
        } ?: throw BadRequestException("Booking could not be found")
    }

    @GetMapping(
            value = "/booking/nonMatching",
            produces = arrayOf(MediaType.APPLICATION_JSON_UTF8_VALUE)
    )
    fun getBooking() : List<Booking> {
        return transaction {
            Booking.find {
                Bookings.matchesExternal eq false
            }.map { it }
        }
    }
}