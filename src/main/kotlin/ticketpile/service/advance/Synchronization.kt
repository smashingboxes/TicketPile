package ticketpile.service.advance

import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.sql.*
import org.joda.time.DateTime
import org.springframework.web.client.HttpClientErrorException
import ticketpile.service.util.ReferenceTable
import ticketpile.service.util.RelationalEntity
import ticketpile.service.util.RelationalTable
import ticketpile.service.util.transaction
import java.sql.Connection

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
val bookingQueueSync = {
    println("Booking Queue sync")
    val tasks = getActiveTasks()
    tasks.forEach { task ->
        val shouldRefresh = transaction(statement = {
            task.bookingsLeft.empty()
        }, logging = false)
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
    val tasks = getActiveTasks()
    tasks.forEach { task ->
        val taskBooking = getTaskBooking(task)
        if (taskBooking != null) {
            val manager = AdvanceLocationManager(task.advanceHost, task.advanceAuthKey, task.advanceLocationId)
            try {
                println("Advance sync: Importing booking ${taskBooking.reservationId} from ${task.advanceHost}")
                val advanceReservation = manager.getAdvanceBooking(taskBooking.reservationId)
                manager.importDiscountRules(advanceReservation)
                manager.importAddOns(advanceReservation)
                transaction(statement =  {
                    manager.importByAdvanceReservation(advanceReservation)
                }, logging = false, isolationLevel = Connection.TRANSACTION_SERIALIZABLE)
                transaction(statement = {
                    AdvanceSyncTaskBookings.deleteWhere {
                        AdvanceSyncTaskBookings.id eq taskBooking.id
                    }
                }, logging = false)
            } catch(t: HttpClientErrorException) {
                if (t.rawStatusCode == 401)
                    task.updateAuthentication()
            } finally {
                transaction {
                    AdvanceSyncTaskBookings.update({ AdvanceSyncTaskBookings.id eq taskBooking.id }) {
                        it[locked] = false
                    }
                }
            }
        }
    }
}

private fun getActiveTasks() : Iterable<AdvanceSyncTask> {
    return transaction(statement = {
        AdvanceSyncTask.find{ AdvanceSyncTasks.authenticated eq true }.toList()
    }, logging = false)
}

private fun getTaskBooking(task: AdvanceSyncTask) : AdvanceSyncTaskBooking? {
    val id = transaction(statement = {
        val result = AdvanceSyncTaskBookings.select {
            (AdvanceSyncTaskBookings.parent eq task.id) and (AdvanceSyncTaskBookings.locked eq false)
        }.limit(1).forUpdate().firstOrNull()?.get(AdvanceSyncTaskBookings.id)
        if(result != null) {
            AdvanceSyncTaskBookings.update({ AdvanceSyncTaskBookings.id eq result }) {
                it[locked] = true
            }
        }
        result
    }, logging = false, isolationLevel = Connection.TRANSACTION_SERIALIZABLE)
    if(id != null)
        return transaction {
            AdvanceSyncTaskBooking.findById(id)
        }
    return null
}

// Initialization
internal val tables = arrayOf(AdvanceSyncTasks, AdvanceSyncTaskBookings, AdvanceSyncErrors)
fun initializeSynchronization() {
    SchemaUtils.createMissingTablesAndColumns(*tables)
}
