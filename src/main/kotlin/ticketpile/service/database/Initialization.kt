package ticketpile.service.database

import org.jetbrains.exposed.sql.SchemaUtils

/**
 * Created by jonlatane on 9/1/16.
 */

internal val tables = arrayOf(
        Bookings,
        Events,
        Tickets,
        PersonCategories,
        Products,
        AddOns,
        Discounts,
        DiscountPersonCategories,
        DiscountProducts,
        BookingAddOns,
        BookingDiscounts,
        BookingManualAdjustments,
        BookingFees,
        BookingItemAddOns,
        TicketBookingAddOns,
        TicketBookingDiscounts,
        TicketBookingManualAdjustments,
        TicketBookingFees,
        TicketBookingItemAddOns
)

fun initializeModel() {
    SchemaUtils.createMissingTablesAndColumns(*tables)
}