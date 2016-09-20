package ticketpile.service.advance

import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.sql.SchemaUtils
import org.joda.time.DateTime
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
}


object AdvanceSyncTaskBookings : ReferenceTable("advanceSyncTaskBooking", AdvanceSyncTasks) {
    val reservationId = integer("reservationId")
}

//Entities
class AdvanceSyncTask(id: EntityID<Int>) : RelationalEntity(id) {
    companion object : IntEntityClass<AdvanceSyncTask>(AdvanceSyncTasks)

    @get:JsonProperty
    var advanceHost by AdvanceSyncTasks.advanceHost
    @get:JsonProperty
    var advanceAuthKey by AdvanceSyncTasks.advanceAuthKey
    @get:JsonProperty
    var advanceLocationId by AdvanceSyncTasks.advanceLocationId
    @get:JsonProperty
    val bookingQueue by children(AdvanceSyncTaskBooking)
    @get:JsonProperty
    var lastRefresh by AdvanceSyncTasks.lastRefresh
    @get:JsonProperty
    var advanceUser by AdvanceSyncTasks.advanceUser
    var advancePassword by AdvanceSyncTasks.advancePassword
}

class AdvanceSyncTaskBooking(id: EntityID<Int>) : RelationalEntity(id) {
    companion object : IntEntityClass<AdvanceSyncTaskBooking>(AdvanceSyncTaskBookings)
    var task by AdvanceSyncTask referencedOn AdvanceSyncTaskBookings.parent
    @get:JsonProperty
    var reservationId by AdvanceSyncTaskBookings.reservationId
}

// Job Runnables
val bookingQueueSync = {
    println("Booking Queue sync")
    try {
        val tasks = transaction {
            AdvanceSyncTask.all().map{it}
        }
        for (task in tasks) {
            transaction {
                if (task.bookingQueue.isEmpty()) {
                    val manager = AdvanceLocationManager(
                            task.advanceHost,
                            task.advanceAuthKey,
                            task.advanceLocationId
                    )
                    manager.queueAllBookingsForImport()
                }
            }
        }
    } catch(t : Throwable) {
        println("Error in Booking Queue sync: $t")
    }
}

val individualBookingSync = {
    println("Individual booking sync")
    try {
        val tasks = transaction {
            AdvanceSyncTask.all().map{it}
        }
        for (task in tasks) {
            transaction {
                val reservation = task.bookingQueue.firstOrNull()
                if (reservation != null) {
                    println("Advance sync: Importing booking ${reservation.reservationId} from ${task.advanceHost}")
                    val manager = AdvanceLocationManager(task.advanceHost, task.advanceAuthKey, task.advanceLocationId)
                    val advanceReservation = manager.getAdvanceBooking(reservation.reservationId)
                    manager.importByAdvanceReservation(advanceReservation)
                    reservation.delete()
                }
            }
        }
    } catch(t : Throwable) {
        println("Error in booking sync: $t")
    }
}

// Initialization
internal val tables = arrayOf(AdvanceSyncTasks, AdvanceSyncTaskBookings)
fun initializeSynchronization() {
    SchemaUtils.createMissingTablesAndColumns(*tables)
}
