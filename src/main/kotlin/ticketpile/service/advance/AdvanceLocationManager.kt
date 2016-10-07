package ticketpile.service.advance

import org.jetbrains.exposed.dao.EntityCache
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.TransactionManager
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
import java.sql.Connection

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

        return transaction(statement = {
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
        }, isolationLevel = Connection.TRANSACTION_SERIALIZABLE)
    }
    
    fun getAdvanceBooking(reservationId: Int) : AdvanceReservation {
        return api20Request(
                "/bookings/$reservationId",
                AdvanceReservationResponse::class.java).booking
    }
    
    fun importByAdvanceReservation(reservation:AdvanceReservation) : Booking {
        
        //Delete the booking if it already exists.
        Booking.find {
            (Bookings.externalSource eq source) and
                    (Bookings.externalId eq reservation.bookingID)
        }.firstOrNull()?.delete()

        val bookingCustomer = importCustomer(reservation.customer)
        val booking = Booking.new {
            code = reservation.bookingCode
            externalId = reservation.bookingID
            externalSource = source
            customer = bookingCustomer
            status = reservation.bookingStatus
        }
        importDiscounts(booking, reservation)
        importBookingAddOns(booking, reservation)
        importManualAdjustments(booking, reservation)
        importBookingItems(booking, reservation)

        flushEntityCache()
        TicketAdjustmentTransform.transform(booking)

        flushEntityCache()
        booking.matchesExternal = isValid(booking, reservation)
        return booking
    }
    
    private fun flushEntityCache() {
        EntityCache.getOrCreate(TransactionManager.current()).flush()
    }

    fun importDiscountRules(reservation : AdvanceReservation) {
        reservation.pricing.priceAdjustments.filter{it.type == 1500}
                .forEach{
                    aDiscount ->
                    importDiscount(aDiscount.promotionID!!)
                }
    }
    private fun importDiscount(
            promotionId: Int
    ) {
        val aDiscount = api20Request(
                "/promotions/$promotionId?promotionID=$promotionId",
                AdvancePromotionResponse::class.java
        ).promotion
        val calcbasis = DiscountBasis.valueOf(aDiscount.calcbasis.toUpperCase())
        transaction {
            val tDiscount = Discount.find { 
                (Discounts.externalSource eq source) and 
                        (Discounts.externalId eq promotionId)
            }.firstOrNull() ?: Discount.new {
                externalSource = source
                externalId = promotionId
                name = aDiscount.promotionCode
                description = aDiscount.description ?: ""
                basis = calcbasis
            }
            tDiscount.name = aDiscount.promotionCode
            tDiscount.description = aDiscount.description ?: ""
            DiscountPersonCategories.deleteWhere { 
                DiscountPersonCategories.parent eq tDiscount.id
            }
            DiscountProducts.deleteWhere { 
                DiscountProducts.parent eq tDiscount.id
            }
            aDiscount.associations.filter{
                it.productID != null
                && it.selected == true
            }.forEach { 
                association ->
                val tProduct = getProduct(association.productID!!)!!
                DiscountProduct.new {
                    discount = tDiscount
                    product = tProduct
                }
            }
            aDiscount.personCategories.filter {
                it.label != null
                && it.selected == true
            }.forEach {
                category ->
                val tCategory = getPersonCategory(category.personCategoryIndex!!)!!
                DiscountPersonCategory.new {
                    discount = tDiscount
                    personCategory = tCategory
                }
            }
        }
    }

    private fun importDiscounts(
            targetBooking: Booking,
            reservation: AdvanceReservation
    ) {
        // Advance tries to break down discounts on its own.
        // Undo that work, TicketPile is designed to do this better.
        val groupedDiscounts = reservation.pricing.priceAdjustments
                .filter { it.type == 1500 }.groupBy { it.promotionID }.values.map {
            it.reduce { priceAdjustment1, priceAdjustment2 ->
                AdvancePriceAdjustment(
                        amount = priceAdjustment1.amount + priceAdjustment2.amount,
                        label = priceAdjustment1.label,
                        promotionID = priceAdjustment1.promotionID,
                        type = priceAdjustment1.type
                )
            }
        }
        
        groupedDiscounts.forEach{
            aDiscount ->
            val discountUsed = Discount.find { 
                (Discounts.externalSource eq source) and 
                        (Discounts.externalId eq aDiscount.promotionID)
            }.first()
            BookingDiscount.new {
                booking = targetBooking
                amount = aDiscount.amount
                discount = discountUsed
            }
        }
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

    fun importPersonCategories() {
        val personCategoryResponse = api20Request(
                "/personcategories?merchantID=$locationId",
                AdvancePersonCategoryResponse::class.java)
        personCategoryResponse.personCategories.forEach {
            aPersonCategory: AdvancePersonCategory ->
            val externalPCId = getExternalPersonCategoryId(aPersonCategory.personCategoryIndex)
            val personCategory = getPersonCategory(aPersonCategory.personCategoryIndex) 
                    ?: PersonCategory.new {
                externalId = externalPCId
                externalSource = source
                name = aPersonCategory.label ?: "unnamed"
                description = aPersonCategory.categoryDescription
            }
            personCategory.name = aPersonCategory.label ?: "unnamed"
            personCategory.description = aPersonCategory.categoryDescription
        }
    }

    fun importProducts() {
        val productsResponse = api20Request(
                "/merchants/$locationId/products?merchantID=$locationId",
                AdvanceProductsReponse::class.java)
        productsResponse.products.forEach {
            aProduct: AdvanceProduct ->
            importProduct(aProduct)
        }
    }
    
    private fun getProduct(productId : Int) : Product? {
        return Product.find {
            (Products.externalSource eq source) and
                    (Products.externalId eq productId)
        }.firstOrNull()
    }

    private fun getPersonCategory(personCategoryIndex : Int) : PersonCategory? {
        return PersonCategory.find {
            (PersonCategories.externalSource eq source) and
                    (PersonCategories.externalId eq getExternalPersonCategoryId(personCategoryIndex))
        }.firstOrNull()
    }

    private fun importProduct(
            aProduct: AdvanceProduct
    ) {
        println("Advance ProductID: ${aProduct.productID}")
        val product = getProduct(aProduct.productID)
                ?: Product.new {
            externalId = aProduct.productID
            externalSource = source
            name = aProduct.name
            description = aProduct.shortDescription
        }
        product.name = aProduct.name
        product.description = aProduct.shortDescription
    }

    private fun importBookingItems(
            targetBooking: Booking,
            reservation: AdvanceReservation
    ) {
        reservation.bookingItems.forEach {
            aBookingItem: AdvanceBookingItem ->
            val targetEvent = importAvailability(aBookingItem.availabilityID)
            val bookingItem = BookingItem.new {
                booking = targetBooking
                event = targetEvent
                externalSource = source
                externalId = aBookingItem.bookingItemID
            }
            importBookingItemAddOns(aBookingItem, bookingItem)
            importTickets(aBookingItem, bookingItem)
        }
    }
    
    private fun importBookingItemAddOns(
            aBookingItem: AdvanceBookingItem,
            bookingItem: BookingItem
    ) {
        aBookingItem.addonSelections.forEach {
            advanceAddOnSelection ->
            advanceAddOnSelection.options.filter {
                it.label != null
            }.forEach {
                option ->
                val addOnSelection = AddOn.find {
                    (AddOns.externalSource eq source) and
                            (AddOns.externalId eq option.optionID)
                }.firstOrNull() ?: AddOn.new {
                    name = option.label!!
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

    private data class PersonCategoryTicketData(
            val ticketPrice: BigDecimal,
            val totalTickets: Int,
            var ticketsCreated: Int = 0
    )
    
    private fun importTickets(
            aBookingItem: AdvanceBookingItem,
            targetBookingItem: BookingItem
    ) {
        val ticketData = generatePersonCategoryTicketData(aBookingItem)
        aBookingItem.ticketCodes.forEach { 
            aTicketCode ->
            val thePersonCategory = getPersonCategory(aTicketCode.personCategoryIndex)!!
            val personCategoryData = ticketData[thePersonCategory]
            if(personCategoryData != null && personCategoryData.ticketsCreated < personCategoryData.totalTickets) {
                Ticket.new {
                    code = aTicketCode.code
                    bookingItem = targetBookingItem
                    basePrice = personCategoryData.ticketPrice
                    personCategory = thePersonCategory
                }
                personCategoryData.ticketsCreated++
            }
        }
        createMissingTickets(targetBookingItem, ticketData)
    }
    
    private fun generatePersonCategoryTicketData(
            aBookingItem: AdvanceBookingItem
    ) : Map<PersonCategory, PersonCategoryTicketData> {
        val result = mutableMapOf<PersonCategory, PersonCategoryTicketData>()
        aBookingItem.lineTotals.filter { it.type == 1101 }.forEach {
            lineTotal ->
            val personCategory = PersonCategory.find {
                (PersonCategories.externalSource eq source) and (PersonCategories.name eq lineTotal.label)
            }.first()
            val ticketPrice = lineTotal.price.divide(BigDecimal(lineTotal.quantity), RoundingMode.HALF_UP)
            result[personCategory] = PersonCategoryTicketData(ticketPrice, lineTotal.quantity)
        }
        return result
    }
    
    private fun createMissingTickets(
            targetBookingItem: BookingItem,
            ticketMetadata: Map<PersonCategory, PersonCategoryTicketData>
    ) {
        ticketMetadata.forEach { 
            val thePersonCategory = it.key
            val ticketData = it.value
            while(ticketData.ticketsCreated < ticketData.totalTickets) {
                Ticket.new {
                    code = "No Ticket Code Generated"
                    bookingItem = targetBookingItem
                    basePrice = ticketData.ticketPrice
                    personCategory = thePersonCategory
                }
                ticketData.ticketsCreated++
            }
        }
    }

    private fun importAvailability(
            availabilityId: Int
    ): Event {
        val availability = api20Request(
                "/merchants/$locationId/calendars/$availabilityId",
                AdvanceAvailabilityResponse::class.java).calendarEntry
        val eventProduct = getProduct(availability.productID)!!
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
