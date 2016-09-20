package ticketpile.service.advance

import AdvanceSyncTask
import ticketpile.service.util.transaction

/**
 * Created by jonlatane on 9/19/16.
 */

val bookingQueueSync = {
    println("Booking Queue sync")
    try {
        val tasks = transaction {
            AdvanceSyncTask.Companion.all().map{it}
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
            AdvanceSyncTask.Companion.all().map{it}
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