
import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.sql.SchemaUtils
import org.joda.time.DateTime
import ticketpile.service.database.ReferenceTable
import ticketpile.service.database.RelationalTable
import ticketpile.service.util.RelationalEntity

/**
 * Two tables and their entities, and a job to import them.
 */

object AdvanceSyncTasks : RelationalTable("advanceSyncTask") {
    val advanceHost = varchar("advanceHost", length = 128)
    val advanceAuthKey = varchar("advanceAuthKey", length = 128)
    val advanceLocationId = integer("advanceLocationId")
    val advanceUser = varchar("advanceUser", length = 128).nullable()
    val advancePassword = varchar("advancePassword", length = 128).nullable()
    val lastRefresh = datetime("lastRefresh").default(
            //DateTime.parse("1970-01-01T00:00:00")
    DateTime(0)
    )
}


object AdvanceSyncTaskBookings : ReferenceTable("advanceSyncTaskBooking", AdvanceSyncTasks) {
    val reservationId = integer("reservationId")
}

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

internal val tables = arrayOf(AdvanceSyncTasks, AdvanceSyncTaskBookings)

fun initializeSynchronization() {
    SchemaUtils.createMissingTablesAndColumns(*tables)
}
