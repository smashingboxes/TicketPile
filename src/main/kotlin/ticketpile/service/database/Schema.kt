package ticketpile.service.database

import ticketpile.service.model.AddOnBasis
import ticketpile.service.model.DiscountBasis
import ticketpile.service.util.BigZero
import ticketpile.service.util.ReferenceTable
import ticketpile.service.util.RelationalTable


/**
 * JetBrains Exposed DAO structure for TicketPile.
 * 
 * Created by jonlatane on 8/26/16.
 */
// Primary entities
object Bookings : RelationalTable("booking") {
    val code = varchar("name", length = 128).index()
    val status = varchar("status", length = 128).index()
    val customer = reference(Customers).index()
    val matchesExternal = bool("matchesExternal").default(false).index()
    val locationId = integer("location")
    val taxAmount = decimal("taxAmount").default(BigZero)
    
    //Caching columns
    val basePrice = decimal("basePrice").nullable()
    val discountsAmount = decimal("discountsAmount").nullable()
    val feesAmount = decimal("feesAmount").nullable()
    val addOnsAmount = decimal("addOnsAmount").nullable()
    val manualAdjustmentsAmount = decimal("manualAdjustmentsAmount").nullable()
    val itemAddOnsAmount = decimal("itemAddOnsAmount").nullable()
    val grossAmount = decimal("grossAmount").nullable() //Sum of the above
    val totalAmount = decimal("totalAmount").nullable() //

    /**
     * @see [ticketpile.service.graphql.BookingSearch.bookingOp]
     */
    val idxGraphQL = index(false, locationId, externalId, status, code)
}

object Events : RelationalTable("event") {
    val capacity = integer("capacity")
    val startTime = datetime("startTime")
    val endTime = datetime("endTime")
    val product = reference(Products)
    val locationId = integer("location")
}

object BookingItems : RelationalTable("bookingItem") {
    val event = reference("event", Events)
    val booking = reference("booking", Bookings)

    //Caching columns
    val basePrice = decimal("basePrice").nullable()
    val discountsAmount = decimal("discountsAmount").nullable()
    val feesAmount = decimal("feesAmount").nullable()
    val addOnsAmount = decimal("addOnsAmount").nullable()
    val manualAdjustmentsAmount = decimal("manualAdjustmentsAmount").nullable()
    val itemAddOnsAmount = decimal("itemAddOnsAmount").nullable()
    val grossAmount = decimal("grossAmount").nullable()
    
    val totalAmount = decimal("totalAmount").nullable()
}

object Tickets : ReferenceTable("ticket", BookingItems) {
    val personCategory = reference(PersonCategories)
    val basePrice = decimal("baseprice")
    val code = varchar("code", length = 128)
    
    // Caching columns
    val discountsAmount = decimal("discountsAmount").nullable()
    val feesAmount = decimal("feesAmount").nullable()
    val addOnsAmount = decimal("addOnsAmount").nullable()
    val manualAdjustmentsAmount = decimal("manualAdjustmentsAmount").nullable()
    val itemAddOnsAmount = decimal("itemAddOnsAmount").nullable()
    val grossAmount = decimal("grossAmount").nullable()
    val totalAmount = decimal("totalAmount").nullable()
    
    // More caching columns useful for analysis
    val discountCount = integer("discountCount").nullable()
    val feeCount = integer("feeCount").nullable()
    val addOnCount = integer("addOnCount").nullable()
    val manualAdjustmentCount = integer("manualAdjustmentCount").nullable()
    val itemAddOnCount = integer("itemAddOnCount").nullable()
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
    val basis = enumeration("basis", AddOnBasis::class.java)
}

object Discounts : RelationalTable("discount") {
    val name = varchar("name", length = 128)
    val description = varchar("description", length = 1024)
    val basis = enumeration("basis", DiscountBasis::class.java)
    //val percentOff = decimal("percentOff", precision = 16, scale = 16).nullable()
    //val discountAmount = decimal("discountAmount", precision = 16, scale = 16).nullable()
}

object DiscountPersonCategories : ReferenceTable("discountPersonCategory", Discounts) {
    val personCategory = reference(PersonCategories)
}

object DiscountProducts : ReferenceTable("discountProduct", Discounts) {
    val product = reference(Products)
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

// Adjustments allowed by Advance's data model
object BookingAddOns : AddOnTable("bookingAddOn", Bookings)
object BookingDiscounts : DiscountTable("bookingDiscount", Bookings)
object BookingManualAdjustments : ManualAdjustmentTable("bookingManualAdjustment", Bookings)
object BookingFees : FeeTable("bookingFee", Bookings)
object BookingItemAddOns : AddOnTable("bookingItemAddOn", BookingItems)

// Mappings of Booking/BookingItem adjustment amounts to Tickets
// Allows us to define programmatically how ticket prices are affected
// by addons and discounts
object TicketBookingAddOns : AddOnTable("ticketBookingAddOn", Tickets) {
    val bookingAddOn = reference(BookingAddOns)
}
object TicketBookingDiscounts : DiscountTable("ticketBookingDiscount", Tickets) {
    val bookingDiscount = reference(BookingDiscounts)
}
object TicketBookingManualAdjustments : ManualAdjustmentTable("ticketBookingManualAdjustment", Tickets) {
    val bookingManualAdjustment = reference(BookingManualAdjustments)
}
object TicketBookingFees : FeeTable("ticketBookingFee", Tickets) {
    val bookingFee = reference(BookingFees)
}
object TicketBookingItemAddOns : AddOnTable("ticketBookingItemAddOn", Tickets) {
    val bookingItemAddOn = reference(BookingItemAddOns)
}