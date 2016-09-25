package ticketpile.service.database


/**
 * JetBrains Exposed DAO structure for TicketPile.
 * 
 * Created by jonlatane on 8/26/16.
 */
// Primary entities
object Bookings : RelationalTable("booking") {
    val code = varchar("name", length = 128).index()
    val status = varchar("status", length = 128).index()
    val customer = reference("customer", Customers)
}

object Events : RelationalTable("event") {
    val capacity = integer("capacity")
    val startTime = datetime("startTime")
    val endTime = datetime("endTime")
    val product = reference("product", Products)
}

object BookingItems : RelationalTable("bookingItem") {
    val event = reference("event", Events)
    val booking = reference("booking", Bookings)
}

object Tickets : ReferenceTable("tickets", BookingItems) {
    val personCategory = reference("personCategory", PersonCategories)
    val basePrice = decimal("baseprice", precision = 65, scale = 30)
    val code = varchar("code", length = 128)
}

// Other things tracked by ID
object PersonCategories : RelationalTable("personCategory") {
    val name = varchar("name", length = 128).index()
    val description = varchar("description", length = 1024)
}

object Products : RelationalTable("product") {
    val name = varchar("name", length = 128)
    val description = varchar("description", length = 1024)
}

object AddOns : RelationalTable("addon") {
    val name = varchar("name", length = 128)
    val product = reference("product", Products).nullable()
}

object Discounts : RelationalTable("discount") {
    val name = varchar("name", length = 128)
    val description = varchar("description", length = 1024)
    //val percentOff = decimal("percentOff", precision = 16, scale = 16).nullable()
    //val discountAmount = decimal("discountAmount", precision = 16, scale = 16).nullable()
}

object Customers : RelationalTable("customer") {
    val firstName = varchar("firstName", length = 128).nullable()
    val lastName = varchar("lastName", length = 128).nullable()
    val emailAddress = varchar("emailAddress", length = 128).nullable()
    val state = varchar("state", length = 128).nullable()
    val country = varchar("country", length = 128).nullable()
    val address1 = varchar("address1", length = 128).nullable()
    val address2 = varchar("address2", length = 128).nullable()
}

//Adjustments allowed by data model
object BookingAddOns : AddOnTable("bookingAddOn", Bookings)
object BookingDiscounts : DiscountTable("bookingDiscount", Bookings)
object BookingManualAdjustments : ManualAdjustmentTable("bookingManualAdjustment", Bookings) 

//Event Adjustments relate to an Event and a Booking
//and affect pricing of all Tickets on an Event.
object BookingItemAddOns : AddOnTable("bookingItemAddOns", BookingItems)

// Things not supported by ZOZI
//object TicketAddOns : AddOnTable("ticketAddOn", Tickets)
//object TicketDiscounts : DiscountTable("ticketDiscount", Tickets)
//object TicketManualAdjustments : ManualAdjustmentTable("ticketManualAdjustment", Tickets)

// Mappings of Booking/BookingItem adjustment amounts to Tickets
// Allows us to define programmatically how ticket prices are affected
// by addons and discounts
object TicketBookingAddOns : AddOnTable("ticketBookingAddOn", Tickets) {
    val bookingAddOn = reference("bookingAddOn", BookingAddOns)
}
object TicketBookingDiscounts : DiscountTable("ticketBookingDiscount", Tickets) {
    val bookingDiscount = reference("bookingDiscount", BookingDiscounts)
}
object TicketBookingManualAdjustments : ManualAdjustmentTable("ticketBookingManualAdjustment", Tickets) {
    val bookingManualAdjustment = reference("bookingAddOn", BookingAddOns)
}
object TicketBookingItemAddOns : AddOnTable("ticketBookingItemAddOn", Tickets) {
    val bookingItemAddOn = reference("bookingItemAddOn", BookingItemAddOns)
}