package ticketpile.service.advance

import org.springframework.web.bind.annotation.*
import org.springframework.web.client.HttpClientErrorException
import ticketpile.service.model.Booking
import ticketpile.service.util.BadRequestException
import ticketpile.service.util.transaction

/**
 * Controller capable of receiving Advance reservations and dumping
 * their data into TicketPile.
 * Created by jonlatane on 8/27/16.
 */
@RestController
@RequestMapping(value = "/advance")
open class AdvanceReservationController {
    @PostMapping(value = "/singleImport")
    fun singleImport(
            @RequestParam(value = "advanceHost", required = true)
            host: String,
            @RequestParam(value = "advanceAuthKey", required = true)
            authorizationKey: String,
            @RequestParam(value = "advanceLocationId", required = true)
            locationId: Int,
            @RequestParam(value = "reservationId", required = true)
            reservationId: Int
    ): Booking {
        return AdvanceLocationManager(host, authorizationKey, locationId)
                .importById(reservationId)
    }
    
    @PostMapping(value = "/synchronizeLocation")
    fun synchronizeLocation(
            @RequestParam(value = "advanceHost", required = true)
            host: String,
            @RequestParam(value = "advanceAuthKey", required = false)
            authorizationKey: String?,
            @RequestParam(value = "advanceUser", required = false)
            advanceUser: String?,
            @RequestParam(value = "advancePassword", required = false)
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
        } else if(authorizationKey != null) {
            authKey = authorizationKey
        } else {
            throw BadRequestException("Either a valid auth key or a valid user/password must be provided. " +
                    "An invalid user/password is never acceptable.")
        }
        
        val manager = AdvanceLocationManager(host, authKey, locationId)
        val task = manager.synchronize(
                advanceUser,
                advancePassword
        )
        return task
    }
    
    @GetMapping(value = "/synchronizationQueues")
    fun synchronizationQueues() : Iterable<AdvanceSyncTask> {
        return transaction {
            AdvanceSyncTask.all().map {it}
        }
    }
}