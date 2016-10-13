package ticketpile.service.graphql

import org.jetbrains.exposed.sql.*
import org.joda.time.DateTime
import ticketpile.service.database.BookingItems
import ticketpile.service.database.Bookings
import ticketpile.service.database.Events
import ticketpile.service.model.Booking
import ticketpile.service.util.BigZero
import java.math.BigDecimal

/**
 * Encapsulation of a query for [Booking]s.
 * 
 * Created by jonlatane on 10/11/16.
 */
val minDate = DateTime("1000-01-01T00:00:00")
val maxDate = DateTime("9999-12-31T00:00:00")
class BookingQuery(
        /**
         * When no products are provided, will search *all* products.
         */
        var products : List<Int> = emptyList<Int>(),
        /**
         * When no locations are provided, will search *no* locations.
         */
        var locations : List<Int> = listOf(-1),
        var eventsAfter : DateTime? = null,
        var eventsBefore : DateTime? = null,
        var status : String? = null,
        var code : String? = null,
        var id : Int? = null,
        var limit : Int,
        var offset : Int
) {
    private val eventBookingIds by lazy {
        if(eventsAfter != null || eventsBefore != null)
            (Events innerJoin BookingItems innerJoin Bookings)
                    .slice(Events.id, BookingItems.event, BookingItems.booking, Bookings.id)
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
                it[Bookings.id].value to it[Events.id].value
            }
        else null
    }
    val bookingOp : SqlExpressionBuilder.()-> Op<Boolean> = {
        Bookings.locationId inList locations and
        (if(eventBookingIds != null)
            Bookings.externalId inList eventBookingIds?.map { it.first }!!
        else if (id != null)
            Bookings.externalId eq id
        else
            Bookings.externalId neq -1) and
        (if (status != null)
            Bookings.status eq status!!
        else
            Bookings.status neq "BookingStatusThatCan'tExist") and
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
    val totalGross : BigDecimal by lazy {
        Bookings.slice(Bookings.bookingTotal.sum())
            .select(bookingOp)
                .first()[Bookings.bookingTotal.sum()]!!
        /*result.map{it.bookingTotal!!}.fold(
                initial = BigZero,
                operation = {
                    amount1, amount2 -> amount1 + amount2
                })*/
    }
    val pageGross : BigDecimal by lazy {
        results.map { it.bookingTotal!! }.fold(
                initial = BigZero,
                operation = {
                    amount1, amount2 ->
                    amount1 + amount2
                })
    }
}