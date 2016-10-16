package ticketpile.service.graphql

import org.jetbrains.exposed.sql.*
import org.joda.time.DateTime
import ticketpile.service.database.BookingItems
import ticketpile.service.database.Bookings
import ticketpile.service.database.Events
import ticketpile.service.model.Booking
import ticketpile.service.model.Ticket
import ticketpile.service.model.transformation.Weighable
import ticketpile.service.util.BigZero
import java.math.BigDecimal

/**
 * Encapsulation of a query for [Booking]s.
 * 
 * Created by jonlatane on 10/11/16.
 */
val minDate = DateTime("1000-01-01T00:00:00")
val maxDate = DateTime("9999-12-31T00:00:00")

class BookingSearch (
        /**
         * When no locations are provided, will search *no* locations.
         */
        var locations : List<Int>,
        var eventsAfter : DateTime?,
        var eventsBefore : DateTime?,
        var status : List<String>,
        var code : String?,
        var id : Int?,
        var limit : Int,
        var offset : Int
) : Weighable {
    private val eventBookingIds : Set<Int>? by lazy {
        // If Event date ranges were used, precompute
        // the valid bookings. This may be optimized later,
        // if necessary.
        if(eventsAfter != null || eventsBefore != null) {
        val result = setOf(
                *(Events innerJoin BookingItems innerJoin Bookings)
                .slice(Events.id, BookingItems.event, BookingItems.booking, Bookings.id, Bookings.externalId)
                .select {
                    Events.locationId inList locations and
                    (if (eventsAfter != null)
                        Events.startTime greaterEq eventsAfter!!
                    else
                        Events.startTime greaterEq minDate) and
                    (if (eventsBefore != null)
                        Events.startTime lessEq eventsBefore!!
                    else
                        Events.startTime lessEq maxDate)
                }.map {
                it[Bookings.externalId]!!
            }.toTypedArray())
            if(id != null) {
                result.intersect(setOf(id!!))
            } else {
                result
            }
        } else null
    }
    private val bookingOp : SqlExpressionBuilder.() -> Op<Boolean> = {
        Bookings.locationId inList locations and
        (if(eventBookingIds != null)
            Bookings.externalId inList eventBookingIds!!.toList()
        else if (id != null)
            Bookings.externalId eq id
        else
            Bookings.externalId neq -1) and
        (if(status.isNotEmpty())
            Bookings.status inList status
        else
            Bookings.status neq "StatusThatCan'tExist") and
        (if (code != null)
            Bookings.code eq code!!
        else
            Bookings.code neq "BookingCodeThatCan'tExist")
    }
    private val result = Booking.find(bookingOp)
    val results: List<Booking> by lazy {
        result.limit(limit, offset = offset).toList()
    }
    val totalCount by lazy {
        result.count()
    }
    fun total(column : Column<BigDecimal?>) : BigDecimal {
        return Bookings.slice(column.sum())
                .select(bookingOp)
                .first()[column.sum()]!!
    }
    override val totalAmount: BigDecimal by lazy {
        total(Bookings.totalAmount)
    }
    override val basePrice: BigDecimal by lazy {
        total(Bookings.basePrice)
    }
    override val discountsAmount: BigDecimal by lazy {
        total(Bookings.discountsAmount)
    }
    override val feesAmount: BigDecimal by lazy {
        total(Bookings.feesAmount)
    }
    override val addOnsAmount: BigDecimal by lazy {
        total(Bookings.addOnsAmount)
    }
    override val manualAdjustmentsAmount: BigDecimal by lazy {
        total(Bookings.manualAdjustmentsAmount)
    }
    override val itemAddOnsAmount: BigDecimal by lazy {
        total(Bookings.itemAddOnsAmount)
    }
    override val grossAmount: BigDecimal by lazy {
        total(Bookings.grossAmount)
    }
    override val tickets: List<Ticket>
        get() = throw UnsupportedOperationException()
    
    val pageTotal: BigDecimal by lazy {
        results.map { it.totalAmount!! }.fold(
                initial = BigZero,
                operation = {
                    amount1, amount2 ->
                    amount1 + amount2
                })
    }
}