package ticketpile.service.advance

import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.and
import org.joda.time.DateTime
import org.springframework.web.client.HttpClientErrorException
import ticketpile.service.database.ReferenceTable
import ticketpile.service.database.RelationalTable
import ticketpile.service.util.RelationalEntity
import ticketpile.service.util.transaction

/**
 * Two tables and their entities, and two jobs to be run in the
 * background at whatever rate we like.
 */

//DB Model
object AdvanceSyncTasks : RelationalTable("advanceSyncTask") {
    val advanceHost = varchar("advanceHost", length = 128)
    val advanceAuthKey = varchar("advanceAuthKey", length = 128)
    val advanceLocationId = integer("advanceLocationId")
    val advanceUser = varchar("advanceUser", length = 128).nullable()
    val advancePassword = varchar("advancePassword", length = 128).nullable()
    val lastRefresh = datetime("lastRefresh").default(DateTime(0))
    val authenticated = bool("authenticated").default(false).index()
}

object AdvanceSyncTaskBookings : ReferenceTable("advanceSyncTaskBooking", AdvanceSyncTasks) {
    val reservationId = integer("reservationId")
    val locked = bool("locked").default(false)
}

//Entities
class AdvanceSyncTask(id: EntityID<Int>) : RelationalEntity(id) {
    companion object : IntEntityClass<AdvanceSyncTask>(AdvanceSyncTasks)

    @get:JsonProperty
    var advanceHost by AdvanceSyncTasks.advanceHost
    @get:JsonProperty
    var advanceLocationId by AdvanceSyncTasks.advanceLocationId
    
    val bookingsLeft by AdvanceSyncTaskBooking referrersOn AdvanceSyncTaskBookings.parent
    @get:JsonProperty
    val queueSize : Int get() {
        return bookingsLeft.count()
    }
    @get:JsonProperty
    var lastRefresh by AdvanceSyncTasks.lastRefresh
    
    @get:JsonProperty
    var advanceAuthKey by AdvanceSyncTasks.advanceAuthKey
    @get:JsonProperty
    var advanceUser by AdvanceSyncTasks.advanceUser
    var advancePassword by AdvanceSyncTasks.advancePassword
    @get:JsonProperty
    var authenticated by AdvanceSyncTasks.authenticated
    
    fun updateAuthentication() {
        if (advanceUser != null && advancePassword != null) {
            println("Getting fresh auth key for $advanceUser on $advanceHost")
            try {
                val newKey = AdvanceLocationManager.getAuthKey(
                        advanceHost,
                        advanceUser!!,
                        advancePassword!!
                )
                advanceAuthKey = newKey
                authenticated = true
                transaction {
                    val updatedTask = AdvanceSyncTask.find {
                        AdvanceSyncTasks.id eq id
                    }.firstOrNull()
                    updatedTask?.advanceAuthKey = newKey
                    updatedTask?.authenticated = true
                }
            } catch(t: Throwable) {
                println("Failed to login as $advanceUser on $advanceHost")
            }
        }
    }
}

class AdvanceSyncTaskBooking(id: EntityID<Int>) : RelationalEntity(id) {
    companion object : IntEntityClass<AdvanceSyncTaskBooking>(AdvanceSyncTaskBookings)
    var task by AdvanceSyncTask referencedOn AdvanceSyncTaskBookings.parent
    @get:JsonProperty
    var reservationId by AdvanceSyncTaskBookings.reservationId
    @get:JsonProperty
    var locked by AdvanceSyncTaskBookings.locked
}

// Job Logic
private fun getActiveTasks() : Iterable<AdvanceSyncTask> {
    return transaction(statement = {
        AdvanceSyncTask.find{ AdvanceSyncTasks.authenticated eq true }.forUpdate().map{it}
    }, logging = false)
}

val bookingQueueSync = {
    println("Booking Queue sync")
    val tasks = getActiveTasks()
    tasks.forEach { task ->
        val shouldRefresh = transaction {
            task.bookingsLeft.empty()
        }
        if (shouldRefresh) {
            println("Looking for updated bookings on ${task.advanceHost} " +
                    "for location ${task.advanceLocationId} as user " +
                    "${task.advanceUser} and token ${task.advanceAuthKey}.")
            val manager = AdvanceLocationManager(
                    task.advanceHost,
                    task.advanceAuthKey,
                    task.advanceLocationId
            )
            try {
                manager.synchronize(task)
            } catch(t: HttpClientErrorException) {
                if (t.rawStatusCode == 401)
                    task.updateAuthentication()
            }
        }
    }
}

val individualBookingSync = {
    //println("Individual Booking sync")
    val tasks = getActiveTasks()
    tasks.forEach { task ->
        val taskBooking = transaction(statement = {
            val booking = AdvanceSyncTaskBooking.find {
                (AdvanceSyncTaskBookings.parent eq task.id) and (AdvanceSyncTaskBookings.locked eq false)
            }.forUpdate().firstOrNull()
            booking?.locked = true
            booking
        }, logging = false)
        if (taskBooking != null) {        
            println("Syncing booking ${taskBooking.reservationId} from ${task.advanceHost}")
            val manager = AdvanceLocationManager(task.advanceHost, task.advanceAuthKey, task.advanceLocationId)
            try {
                val advanceReservation = manager.getAdvanceBooking(taskBooking.reservationId)
                transaction {
                    println("Advance sync: Importing booking ${taskBooking.reservationId} from ${task.advanceHost}")
                    manager.importByAdvanceReservation(advanceReservation)
                    taskBooking.delete()
                }
            } catch(t: HttpClientErrorException) {
                if (t.rawStatusCode == 401)
                    task.updateAuthentication()
            } catch(t: Throwable) {
                println("Error in booking sync: $t")
            } finally {
                transaction {
                    AdvanceSyncTaskBooking.find {
                        AdvanceSyncTaskBookings.id eq taskBooking.id
                    }.firstOrNull()?.locked = false
                }
            }
        }
    }
}

// Initialization
internal val tables = arrayOf(AdvanceSyncTasks, AdvanceSyncTaskBookings)
fun initializeSynchronization() {
    SchemaUtils.createMissingTablesAndColumns(*tables)
}
