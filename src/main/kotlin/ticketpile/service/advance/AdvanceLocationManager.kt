package ticketpile.service.advance

import AdvanceSyncTask
import AdvanceSyncTaskBooking
import AdvanceSyncTasks
import org.jetbrains.exposed.sql.and
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
    }
    constructor(host: String, authorizationKey: String, locationId: Int) {
        if (authorizationKey.startsWith("Bearer:")) authKey = authorizationKey.substring(7)
        else authKey = authorizationKey
        this.locationId = locationId
        source = URL(URL(host), "/").toString()
    }

    private val source: String
    private val authKey: String
    private val locationId: Int
    
    fun queueAllBookingsForImport() : AdvanceSyncTask {
        val bookings = api20Request(
                "/bookings/modified",//?nolines=${Int.MAX_VALUE}",
                AdvanceModifiedBookingsResponse::class.java
        ).bookingIds

        return transaction {
            importProducts()
            importPersonCategories()
            val importTask = AdvanceSyncTask.find {
                (AdvanceSyncTasks.advanceHost eq source) and 
                (AdvanceSyncTasks.advanceLocationId eq locationId)
            }.firstOrNull() ?: AdvanceSyncTask.new {
                advanceHost = source
                advanceAuthKey = authKey
                advanceLocationId = locationId
            }
            importTask.advanceAuthKey = authKey
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

        importBookingItems(booking, reservation)
        return booking
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
        println("WebReserv ProductID: ${wrProduct.productID}")
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

    private fun importBookingItems(
            targetBooking: Booking,
            reservation: AdvanceReservation
    ) {
        targetBooking.items.forEach { 
            it.tickets.forEach(Ticket::delete)
            it.delete()
        }
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
            importTickets(wrBookingItem, bookingItem)
        }
    }
    
    private fun importTickets(
            wrBookingItem: AdvanceBookingItem,
            targetBookingItem: BookingItem
    ) {
        val personCategoryTicketPrices = mutableMapOf<PersonCategory, BigDecimal>()
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

    private fun <T> api20Request(
            path: String,
            klass: Class<T>
    ): T {
        val url = URL(URL(source), "/services/api20$path")
        println("Contacting WebReserv via $url")
        val headers = HttpHeaders()
        headers.put("Bearer", listOf(authKey))
        val entity = HttpEntity<String>("parameters", headers)
        val responseEntity = restTemplate.exchange(URI(url.toString()), HttpMethod.GET, entity, klass)
        println(responseEntity.toString())
        return responseEntity.body
    }
}
