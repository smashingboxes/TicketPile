package ticketpile.service.advance

import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.web.client.RestTemplate
import ticketpile.service.database.*
import ticketpile.service.model.*
import ticketpile.service.util.transaction
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.URI
import java.net.URL

/**
 * Handles per-location imports of Advance API20 bookings.
 * 
 * Created by jonlatane on 9/3/16.
 */
class AdvanceLocationManager {
    companion object {
        private val restTemplate = RestTemplate()
        private val dateTimeFormatter = DateTimeFormat.forPattern(
                "yyyy-MM-dd%20HH:mm:ss"
        )
        val syncPeriodSeconds = 5L
        
        fun getAuthKey(host:String, user:String, password:String) : String {
            val url = URL(URL(host), "/services/api20/authorize/login")
            println("Contacting Advance via $url")
            val entity = HttpEntity<AdvanceAuthRequest>(
                    AdvanceAuthRequest(user, password)
            )
            val responseEntity = restTemplate.exchange(
                    URI(url.toString()), 
                    HttpMethod.POST, 
                    entity, 
                    AdvanceAuthResponse::class.java
            )
            println(responseEntity.toString())
            return responseEntity.body.token
        }
        fun toSource(host:String) : String {
            return URL(URL(host), "/").toString()
        }
    }
    constructor(host: String, authorizationKey: String, locationId: Int) {
        if (authorizationKey.startsWith("Bearer:")) authKey = authorizationKey.substring(7)
        else authKey = authorizationKey
        this.locationId = locationId
        source = toSource(host)
    }

    var source: String
    private val authKey: String
    private val locationId: Int
    
    fun synchronize(
            syncTask : AdvanceSyncTask
    ) : AdvanceSyncTask {
        transaction {
            importProducts()
            importPersonCategories()
        }
        
        val bookings = api20Request(
                "/bookings/modified?since=${
                    dateTimeFormatter.print(syncTask.lastRefresh.toDateTime(DateTimeZone.UTC))
                }",
                AdvanceModifiedBookingsResponse::class.java
        ).bookingIds

        return transaction {
            val importTask = AdvanceSyncTask[syncTask.id]
            importTask.lastRefresh = DateTime(DateTimeZone.UTC)
                    .minusSeconds(syncPeriodSeconds.toInt())
            for(bookingId in bookings) {
                AdvanceSyncTaskBooking.new {
                    reservationId = bookingId
                    task = importTask
                }
            }
            importTask
        }
    }

    internal fun importById(
            reservationId: Int
    ): Booking {
        val reservation = getAdvanceBooking(reservationId)
        println(reservation.bookingCode)

        var result: Booking? = null
        transaction {
            importProducts()
            importPersonCategories()
            result = importByAdvanceReservation(reservation)
        }
        return result!!
    }
    
    fun getAdvanceBooking(reservationId: Int) : AdvanceReservation {
        return api20Request(
                "/bookings/$reservationId",
                AdvanceReservationResponse::class.java).booking
    }
    
    fun importByAdvanceReservation(reservation:AdvanceReservation) : Booking {
        val bookingCustomer = importCustomer(reservation.customer)
        
        val booking = Booking.find {
            (Bookings.externalSource eq source) and
                    (Bookings.externalId eq reservation.bookingID)
        }.firstOrNull() ?: Booking.new {
            code = reservation.bookingCode
            externalId = reservation.bookingID
            externalSource = source
            customer = bookingCustomer
        }
        booking.customer = bookingCustomer
        booking.status = reservation.bookingStatus

        // First remove all booking items and adjustments
        // from the target booking
        prepareImport(booking)
        
        // Import fresh versions of all this data
        importBookingItems(booking, reservation)
        importDiscounts(booking, reservation)
        importBookingAddOns(booking, reservation)
        importManualAdjustments(booking, reservation)
        
        // After importing data for the booking, transform
        // all of the adjustments on it onto its Tickets
        TicketAdjustmentTransform.transform(booking)
        return booking
    }

    private fun importDiscounts(
            targetBooking: Booking,
            reservation: AdvanceReservation
    ) {
        //TODO
    }
    
    private fun importBookingAddOns(
            targetBooking: Booking,
            reservation: AdvanceReservation
    ) {
        //TODO - these do not affect price for TR
    }

    private fun importManualAdjustments(
            targetBooking: Booking,
            reservation: AdvanceReservation
    ) {
        reservation.lineTotals.filter {
            it.type in arrayOf(2000,2100)
        }.forEach {
            BookingManualAdjustment.new {
                subject = targetBooking
                amount = it.price
                description = it.label
            }
        }
    }
    
    private fun importCustomer(advanceCustomer : AdvanceCustomer) : Customer {
        val customer = Customer.find {
            (Customers.externalSource eq source) and
                    (Customers.externalId eq advanceCustomer.customerID)
        }.firstOrNull() ?: Customer.new {
            externalId = advanceCustomer.customerID
            externalSource = source
            firstName = advanceCustomer.firstName
            lastName = advanceCustomer.lastName
            address1 = advanceCustomer.address1
            address2 = advanceCustomer.address2
            state = advanceCustomer.state
            country = advanceCustomer.country
            emailAddress = advanceCustomer.emailAddress
        }
        customer.firstName = advanceCustomer.firstName
        customer.lastName = advanceCustomer.lastName
        customer.address1 = advanceCustomer.address1
        customer.address2 = advanceCustomer.address2
        customer.state = advanceCustomer.state
        customer.country = advanceCustomer.country
        customer.emailAddress = advanceCustomer.emailAddress
        
        return customer
    }
    
    private fun getExternalPersonCategoryId(personCategoryIndex : Int) : Int {
        return (10 * locationId) + personCategoryIndex
    }

    private fun importPersonCategories() {
        val personCategoryResponse = api20Request(
                "/personcategories?merchantID=$locationId",
                AdvancePersonCategoryResponse::class.java)
        personCategoryResponse.personCategories.forEach {
            wrPersonCategory: AdvancePersonCategory ->
            val externalPCId = getExternalPersonCategoryId(wrPersonCategory.personCategoryIndex)
            val personCategory = PersonCategory.find {
                (PersonCategories.externalSource eq source) and
                        (PersonCategories.externalId eq externalPCId)
            }.firstOrNull() ?: PersonCategory.new {
                externalId = externalPCId
                externalSource = source
                name = wrPersonCategory.label ?: "unnamed"
                description = wrPersonCategory.categoryDescription
            }
            personCategory.name = wrPersonCategory.label ?: "unnamed"
            personCategory.description = wrPersonCategory.categoryDescription
        }
    }

    private fun importProducts() {
        val productsResponse = api20Request(
                "/merchants/$locationId/products?merchantID=$locationId",
                AdvanceProductsReponse::class.java)
        productsResponse.products.forEach {
            wrProduct: AdvanceProduct ->
            importProduct(wrProduct)
        }
    }

    private fun importProduct(
            wrProduct: AdvanceProduct
    ) {
        println("Advance ProductID: ${wrProduct.productID}")
        val product = Product.find {
            (Products.externalSource eq source) and
                    (Products.externalId eq wrProduct.productID)
        }.firstOrNull() ?: Product.new {
            externalId = wrProduct.productID
            externalSource = source
            name = wrProduct.name
            description = wrProduct.shortDescription
        }
        product.name = wrProduct.name
        product.description = wrProduct.shortDescription
    }
    
    private fun prepareImport(booking : Booking) {
        booking.items.forEach { 
            bookingItem ->
            bookingItem.tickets.forEach {
                ticket ->
                TicketBookingAddOns.deleteWhere { 
                    TicketBookingAddOns.parent eq ticket.id
                }
                TicketBookingDiscounts.deleteWhere {
                    TicketBookingDiscounts.parent eq ticket.id
                }
                TicketBookingManualAdjustments.deleteWhere {
                    TicketBookingManualAdjustments.parent eq ticket.id
                }
                TicketBookingItemAddOns.deleteWhere {
                    TicketBookingItemAddOns.parent eq ticket.id
                }
                ticket.delete()
            }
            BookingItemAddOns.deleteWhere { 
                BookingItemAddOns.parent eq bookingItem.id
            }
            bookingItem.delete()
        }
        BookingAddOns.deleteWhere {
            BookingAddOns.parent eq booking.id
        }
        BookingDiscounts.deleteWhere {
            BookingDiscounts.parent eq booking.id
        }
        BookingManualAdjustments.deleteWhere {
            BookingManualAdjustments.parent eq booking.id
        }
    }

    private fun importBookingItems(
            targetBooking: Booking,
            reservation: AdvanceReservation
    ) {
        reservation.bookingItems.forEach {
            wrBookingItem: AdvanceBookingItem ->
            val targetEvent = importAvailability(wrBookingItem.availabilityID)
            val bookingItem = BookingItem.find {
                (BookingItems.externalSource eq source) and
                        (BookingItems.externalId eq wrBookingItem.bookingItemID)
            }.firstOrNull() ?: BookingItem.new {
                booking = targetBooking
                event = targetEvent
                externalSource = source
                externalId = wrBookingItem.bookingItemID
            }
            bookingItem.booking = targetBooking
            bookingItem.event = targetEvent
            importBookingItemAddOns(wrBookingItem, bookingItem)
            importTickets(wrBookingItem, bookingItem)
        }
    }
    
    private fun importBookingItemAddOns(
            wrBookingItem: AdvanceBookingItem,
            bookingItem: BookingItem
    ) {
        wrBookingItem.addonSelections.forEach {
            advanceAddOnSelection ->
            advanceAddOnSelection.options.forEach { 
                option ->
                val addOnSelection = AddOn.find {
                    (AddOns.externalSource eq source) and
                            (AddOns.externalId eq option.optionID)
                }.firstOrNull() ?: AddOn.new {
                    name = option.label
                    externalSource = source
                    externalId = option.optionID
                }
                BookingItemAddOn.new {
                    addOn = addOnSelection
                    prompt = advanceAddOnSelection.name
                    amount = option.price
                    subject = bookingItem
                }
            }
        }
    }
    
    private fun importTickets(
            wrBookingItem: AdvanceBookingItem,
            targetBookingItem: BookingItem
    ) {
        val personCategoryTicketPrices = mutableMapOf<PersonCategory, BigDecimal>()
        //var numTickets
        wrBookingItem.lineTotals.filter { it.type == 1101 }.forEach { 
            lineTotal ->
            val personCategory = PersonCategory.find {
                (PersonCategories.externalSource eq source) and (PersonCategories.name eq lineTotal.label)
            }.first()
            val ticketPrice = lineTotal.price.divide(BigDecimal(lineTotal.quantity), RoundingMode.HALF_UP)
            personCategoryTicketPrices[personCategory] = ticketPrice
        }
        wrBookingItem.lineTotals.filter { it.type == 1500 }
        wrBookingItem.ticketCodes.forEach { 
            wrTicketCode ->
            val thePersonCategory = PersonCategory.find {
                (PersonCategories.externalSource eq source) and 
                        (PersonCategories.externalId eq getExternalPersonCategoryId(wrTicketCode.personCategoryIndex))
            }.first()
                    //personCategories[wrTicketCode.personCategoryIndex]!!
            val ticket = Ticket.find {
                (Tickets.externalSource eq source) and
                        (Tickets.externalId eq wrTicketCode.ticketCodeID)
            }.firstOrNull() ?: Ticket.new {
                code = wrTicketCode.code
                bookingItem = targetBookingItem
                basePrice = personCategoryTicketPrices[thePersonCategory] ?: BigDecimal.ZERO
                personCategory = thePersonCategory
            }
            ticket.bookingItem = targetBookingItem
            ticket.code = wrTicketCode.code
            ticket.basePrice = personCategoryTicketPrices[thePersonCategory] ?: BigDecimal.ZERO
            ticket.personCategory = thePersonCategory
        }
    }

    private fun importAvailability(
            availabilityId: Int
    ): Event {
        val availability = api20Request(
                "/merchants/$locationId/calendars/$availabilityId",
                AdvanceAvailabilityResponse::class.java).calendarEntry
        println("Event product ID: ${availability.productID}")
        val eventProduct = Product.find {
            (Products.externalSource eq source) and (Products.externalId eq availability.productID)
        }.first()
        //products[availability.productID]!!
        val event = Event.find {
            (Events.externalSource eq source) and (Events.externalId eq availability.availabilityID)
        }.firstOrNull() ?: Event.new {
            externalId = availability.availabilityID
            externalSource = source
            startTime = availability.startDateTime
            endTime = availability.endDateTime
            capacity = availability.maxCapacity
            product = eventProduct
        }

        event.startTime = availability.startDateTime
        event.endTime = availability.endDateTime
        event.capacity = availability.maxCapacity
        event.product = eventProduct
        return event
    }
    
    val currentUser : AdvanceUser get() {
        return api20Request(
                "/authorization/current-user", 
                AdvanceUserRespose::class.java
        ).user
    }

    private fun <T> api20Request(
            path: String,
            klass: Class<T>
    ): T {
        val url = URL(URL(source), "/services/api20$path")
        println("Contacting Advance via $url")
        val headers = HttpHeaders()
        headers.put("Bearer", listOf(authKey))
        val entity = HttpEntity<String>("parameters", headers)
        val responseEntity = restTemplate.exchange(URI(url.toString()), HttpMethod.GET, entity, klass)
        println(responseEntity.toString())
        return responseEntity.body
    }
}
