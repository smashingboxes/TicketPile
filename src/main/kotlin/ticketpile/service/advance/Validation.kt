package ticketpile.service.advance

import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.exposed.dao.EntityID
import ticketpile.service.database.Bookings
import ticketpile.service.model.Booking
import ticketpile.service.util.ReferenceTable
import ticketpile.service.util.RelationalEntity
import ticketpile.service.util.RelationalEntityClass
import java.math.BigDecimal

/**
 * A few simple methods for validating TicketPile bookings against Advance reservations.
 * 
 * Created by jonlatane on 10/5/16.
 */

enum class SyncErrorLevel() {
    error(),
    warning()
}
enum class SyncErrorType(val description : String, val level: SyncErrorLevel) {
    itemCount("Booking item counts did not match", SyncErrorLevel.error),
    ticketCount("Ticket counts did not match", SyncErrorLevel.error),
    bookingTotal("Booking total did not match", SyncErrorLevel.error),
    missingTicketCodes("Advance didn't generate a ticket code", SyncErrorLevel.warning),
    extraTicketCodes("Advance generated an extra ticket code", SyncErrorLevel.warning),
    mismatchTicketCodes("Advance had ticket codes for a person category not listing in pricing", SyncErrorLevel.warning),
    extraDiscountCodes("Advance listed the same discount on a booking twice", SyncErrorLevel.warning),
    inApplicableAdjustment("A discount or add-on was listed that shouldn't apply to any tickets", SyncErrorLevel.warning),
    missingPersonCategory("A Person Category was listed in pricing information that is not listed in Merchant's Person Categories", SyncErrorLevel.warning),
    unusableTicketCode("A Ticket Code couldn't be allocated to any pricing data and didn't survive the import to TicketPile", SyncErrorLevel.warning);

    companion object {
        val errors : List<SyncErrorType> by lazy {
            SyncErrorType.values().filter { it.level == SyncErrorLevel.error }
        }
        val warnings : List<SyncErrorType> by lazy {
            SyncErrorType.values().filter { it.level == SyncErrorLevel.warning }
        }
    }
}
object AdvanceSyncErrors : ReferenceTable("advanceSyncTask", Bookings) {
    val message = varchar("message", length = 256)
    val errorType = enumeration("errorType", SyncErrorType::class.java)
}

class AdvanceSyncError(id: EntityID<Int>) : RelationalEntity(id) {
    companion object : RelationalEntityClass<AdvanceSyncError>(AdvanceSyncErrors)
    var booking by Booking referencedOn AdvanceSyncErrors.parent
    @get:JsonProperty
    var message by AdvanceSyncErrors.message
    @get:JsonProperty
    var errorType by AdvanceSyncErrors.errorType
}

fun validateImportResult(booking : Booking, reservation : AdvanceReservation) {
    //Test booking item count
    if(booking.items.count() != reservation.bookingItems.count())
        AdvanceSyncError.new {
            errorType = SyncErrorType.itemCount
            this.booking = booking
            message = "TicketPile: ${booking.items.count()}; " +
                    "Advance ${reservation.bookingItems.count()}"
        }
    
    //Test ticket count
    val bookingTicketCount = booking.tickets.count()
    val reservationTicketCount = reservation.bookingItems.fold(0, {
        bookingTicketTotal, advanceBookingItem ->
        bookingTicketTotal + advanceBookingItem.lineTotals.filter { it.type == 1101 }.fold(0, {
            itemTicketTotal, lineTotal ->
            itemTicketTotal + lineTotal.quantity
        })
    })
    if(bookingTicketCount != reservationTicketCount)
        AdvanceSyncError.new {
            errorType = SyncErrorType.ticketCount
            this.booking = booking
            message = "TicketPile: $bookingTicketCount; " +
                    "Advance: $reservationTicketCount"
        }
    
    //Test booking total of confirmed bookings.  Bookings that should calculate as negative are expected
    //to calculate as zero from Advance. See booking 3418678/A-1BHNMT from The Ride.
    if(reservation.bookingStatus == "confirmed"
        && (booking.totalAmount!! + booking.taxAmount - reservation.pricing.totalAmount).abs() > BigDecimal(0.00001)
    )
        AdvanceSyncError.new {
            errorType = SyncErrorType.bookingTotal
            this.booking = booking
            message = "TicketPile: $${booking.totalAmount}; " +
                    "Advance: $${reservation.pricing.totalAmount}"
        }
}