package ticketpile.service.webreserv

import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.web.client.RestTemplate
import ticketpile.service.database.*
import ticketpile.service.model.*
import java.math.BigDecimal
import java.net.URI
import java.net.URL
import java.sql.Connection

/**
 * Handles per-location imports of WebReserv bookings.
 * 
 * Created by jonlatane on 9/3/16.
 */
class WRLocationManager {
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
    private val personCategories = mutableMapOf<Int, PersonCategory>()
    private val products = mutableMapOf<Int, Product>()

    fun importReservation(
            reservationId: Int
    ): Booking {
        val reservation = api20Request(
                "/bookings/$reservationId",
                WRReservationResponse::class.java).booking
        println(reservation.bookingCode)

        var result: Booking? = null
        transaction(Connection.TRANSACTION_READ_UNCOMMITTED, 1, {
            importProducts()
            importPersonCategories()
            val booking = Booking.find {
                (Bookings.externalSource eq source) and
                        (Bookings.externalId eq reservation.bookingID)
            }.firstOrNull() ?: Booking.new {
                code = reservation.bookingCode
                externalId = reservation.bookingID
                externalSource = source
            }

            importBookingItems(booking, reservation)
            result = booking
        })
        return result!!
    }

    private fun importPersonCategories() {
        val personCategoryResponse = api20Request(
                "/personcategories?merchantID=$locationId",
                WRPersonCategoryResponse::class.java)
        personCategoryResponse.personCategories.forEach {
            wrPersonCategory: WRPersonCategory ->
            val externalPCId = (10 * locationId) + wrPersonCategory.personCategoryIndex
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
            personCategories.put(wrPersonCategory.personCategoryIndex, personCategory)
        }
    }

    private fun importProducts() {
        val productsResponse = api20Request(
                "/merchants/$locationId/products?merchantID=$locationId",
                WRProductsReponse::class.java)
        productsResponse.products.forEach {
            wrProduct: WRProduct ->
            importProduct(wrProduct)
        }
    }

    private fun importProduct(
            wrProduct: WRProduct
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
        products.put(wrProduct.productID, product)
    }

    private fun importBookingItems(
            targetBooking: Booking,
            reservation: WebReservation
    ) {
        reservation.bookingItems.forEach {
            wrBookingItem: WRBookingItem ->
            val targetEvent = importAvailability(wrBookingItem.availabilityID)
            val bookingItem = BookingItem.find {
                (BookingItems.externalSource eq source) and
                        (BookingItems.externalId eq wrBookingItem.bookingItemID)
            }.firstOrNull() ?: BookingItem.new {
                booking = targetBooking
                event = targetEvent
            }
            importTickets(wrBookingItem, bookingItem)
        }
    }
    
    private fun importTickets(
            wrBookingItem: WRBookingItem,
            targetBookingItem: BookingItem
    ) {
        val personCategoryTicketPrices = mutableMapOf<PersonCategory, BigDecimal>()
        wrBookingItem.lineTotals.filter { it.type == 1101 }.forEach { 
            lineTotal ->
            val personCategory = personCategories.values.filter { 
                it.name == lineTotal.label
            }.first()
            val ticketPrice = lineTotal.price.divide(BigDecimal(lineTotal.quantity))
            personCategoryTicketPrices[personCategory] = ticketPrice
        }
        wrBookingItem.lineTotals.filter { it.type == 1500 }
        wrBookingItem.ticketCodes.forEach { 
            wrTicketCode ->
            val thePersonCategory = personCategories[wrTicketCode.personCategoryIndex]!!
            val ticket = Ticket.find {
                (Tickets.externalSource eq source) and
                        (Tickets.externalId eq wrTicketCode.ticketCodeID)
            }.firstOrNull() ?: Ticket.new {
                code = wrTicketCode.code
                bookingItem = targetBookingItem
                basePrice = personCategoryTicketPrices[thePersonCategory]!!
                personCategory = thePersonCategory
            }
            ticket.bookingItem = targetBookingItem
            ticket.code = wrTicketCode.code
            ticket.basePrice = personCategoryTicketPrices[thePersonCategory]!!
            ticket.personCategory = thePersonCategory
        }
    }

    private fun importAvailability(
            availabilityId: Int
    ): Event {
        val availability = api20Request(
                "/merchants/$locationId/calendars/$availabilityId",
                WRAvailabilityResponse::class.java).calendarEntry
        println("Event product ID: ${availability.productID}")
        val eventProduct = products[availability.productID]!!
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
