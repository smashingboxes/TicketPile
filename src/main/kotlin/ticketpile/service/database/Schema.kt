package ticketpile.service.database

import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.sql.*


/**
 * JetBrains Exposed DAO structure for TicketPile.
 * 
 * Created by jonlatane on 8/26/16.
 */
// Primary entities
object Bookings : RelatableTable("booking") {
    val code = varchar("name", length = 128).index(isUnique = true)
}

object Events : RelatableTable("event") {
    val capacity = integer("capacity")
    val startTime = datetime("startTime")
    val endTime = datetime("endTime")
    val product = reference("product", Products)
}

object Tickets : RelatableTable("ticket") {
    val personCategory = reference("personCategory", PersonCategories)
    val basePrice = decimal("baseprice", precision = 16, scale = 16)
    val event = reference("event", Events)
    val booking = reference("booking", Bookings)
}

// Other things tracked by ID
object PersonCategories : RelatableTable("personCategory") {
    val name = varchar("name", length = 128)
    val description = varchar("description", length = 1024)
}

object Products : RelatableTable("product") {
    val name = varchar("name", length = 128)
    val description = varchar("description", length = 1024)
}

object AddOns : RelatableTable("addon") {
    val name = varchar("name", length = 128)
    val product = reference("product", Products).nullable()
}

object Discounts : RelatableTable("discount") {
    val name = varchar("name", length = 128)
    val description = varchar("description", length = 1024)
    //val percentOff = decimal("percentOff", precision = 16, scale = 16).nullable()
    //val discountAmount = decimal("discountAmount", precision = 16, scale = 16).nullable()
}

//Adjustments allowed by data model
object BookingAddOns : AddOnTable(Bookings)
object BookingDiscounts : DiscountTable(Bookings)
object BookingManualAdjustments : ManualAdjustmentTable(Bookings) 
object TicketAddOns : AddOnTable(Tickets)
object TicketDiscounts : DiscountTable(Tickets)
object TicketManualAdjustments : ManualAdjustmentTable(Tickets)

//Event Adjustments relate to an Event and a Booking
//and affect pricing of all Tickets on an Event.
object EventAddOns : AddOnTable(Events) {
    val booking = reference("booking", Bookings)
}