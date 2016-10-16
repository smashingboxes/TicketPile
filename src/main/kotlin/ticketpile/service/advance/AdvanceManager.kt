package ticketpile.service.advance

import org.jetbrains.exposed.sql.and
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.web.client.RestTemplate
import ticketpile.service.database.*
import ticketpile.service.model.*
import ticketpile.service.util.flushEntityCache
import ticketpile.service.util.transaction
import java.net.URI
import java.net.URL
import java.sql.Connection

/**
 * Base class for managing auth and imports of data from
 * Advance locations.
 * 
 * Created by jonlatane on 10/10/16.
 */
open class AdvanceManager {
    companion object {
        internal val restTemplate = RestTemplate()
        internal val dateTimeFormatter = DateTimeFormat.forPattern(
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

    var source: String
    internal val authKey: String
    internal val locationId: Int

    constructor(host: String, authorizationKey: String, locationId: Int) {
        if (authorizationKey.startsWith("Bearer:")) authKey = authorizationKey.substring(7)
        else authKey = authorizationKey
        this.locationId = locationId
        source = toSource(host)
    }
    
    fun synchronize(
            syncTask : AdvanceSyncTask
    ) : AdvanceSyncTask {
        transaction {
            importProducts()
            importPersonCategories()
        }

        val bookings = api20Request(
                "/bookings/modified?locationID=$locationId&lastModified=${
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
        }, isolationLevel = Connection.TRANSACTION_SERIALIZABLE, logging = false)
    }

    /**
     * Advance Booking Items are hard to get an availability from.  This encapsulates
     * that.  Assumes [AdvanceLocationManager.importRelatedAvailabilities] has been
     * called for Booking Items with valid availabilities.
     */
    internal fun findAvailabilityFor(bookingItem : AdvanceBookingItem) : Event {
        if(bookingItem.availabilityID != 0) {
            return Event.find {
                (Events.externalSource eq source) and
                        (Events.externalId eq bookingItem.availabilityID)
            }.first()
        } else {
            val product = getProduct(bookingItem.productID)
            var event = Event.find {
                (Events.externalSource eq source) and
                        (Events.externalId eq null as Int?) and
                        (Events.startTime eq bookingItem.startDateTime) and
                        (Events.endTime eq bookingItem.endDateTime) and
                        (Events.product eq product.id) and
                        (Events.locationId eq locationId)
            }.firstOrNull()
            if(event == null) {
                event = Event.new {
                    externalId = null
                    externalSource = source
                    startTime = bookingItem.startDateTime
                    endTime = bookingItem.endDateTime
                    capacity = -1
                    this.product = product
                    locationId = this@AdvanceManager.locationId
                }
            }
            return event
        }
    }

    fun importPersonCategories() {
        val personCategoryResponse = api20Request(
                "/personcategories?merchantID=$locationId",
                AdvancePersonCategoryResponse::class.java)
        personCategoryResponse.personCategories.forEach {
            aPersonCategory: AdvancePersonCategory ->
            val externalPCId = getExternalPersonCategoryId(aPersonCategory.personCategoryIndex)
            val personCategory = PersonCategory.find {
                (PersonCategories.externalSource eq source) and
                        (PersonCategories.externalId eq getExternalPersonCategoryId(aPersonCategory.personCategoryIndex))
            }.firstOrNull() ?: PersonCategory.new {
                externalId = externalPCId
                externalSource = source
                name = aPersonCategory.label ?: "unnamed"
                description = aPersonCategory.categoryDescription ?: ""
            }
            personCategory.name = aPersonCategory.label ?: "unnamed"
            personCategory.description = aPersonCategory.categoryDescription ?: ""
        }
    }
    
    fun importProducts() {
        val productsResponse = api20Request(
                "/merchants/$locationId/products?includeDeleted=true&merchantID=$locationId",
                AdvanceProductsResponse::class.java)
        productsResponse.products.forEach {
            aProduct: AdvanceProduct ->
            importProduct(aProduct)
        }
    }    
    private fun importProduct(
            aProduct: AdvanceProduct
    ) {
        println("Advance ProductID: ${aProduct.productID}")
        val product = Product.find {
            (Products.externalSource eq source) and
                    (Products.externalId eq aProduct.productID)
        }.firstOrNull() ?: Product.new {
            externalId = aProduct.productID
            externalSource = source
            name = aProduct.name
            description = aProduct.shortDescription ?: ""
        }
        product.name = aProduct.name
        product.description = aProduct.shortDescription ?: ""
    }

    internal fun getProduct(productId : Int) : Product {
        var result = Product.find {
            (Products.externalSource eq source) and
                    (Products.externalId eq productId)
        }.firstOrNull()
        if(result == null) { // First check for new products.
            importProducts()
            flushEntityCache()
            result = Product.find {
                (Products.externalSource eq source) and
                        (Products.externalId eq productId)
            }.firstOrNull() 
        }
        if(result == null) { // Advance's lack of FKs means a product has been lost completely.
            result = Product.new { 
                externalSource = source
                externalId = productId
                name = "Unreferenced Product"
                description = "A Product that is missing from Advance entirely."
            }
        }
        return result
    }
    
    /**
     * Gets a TicketPile [PersonCategory] given an Advance Person Category Index
     */
    internal fun getPersonCategory(personCategoryIndex : Int) : PersonCategory {
        val result = PersonCategory.find {
            (PersonCategories.externalSource eq source) and
                    (PersonCategories.externalId eq getExternalPersonCategoryId(personCategoryIndex))
        }.firstOrNull()
        if(result == null) {
            importPersonCategories()
            flushEntityCache()
            return PersonCategory.find {
                (PersonCategories.externalSource eq source) and
                        (PersonCategories.externalId eq getExternalPersonCategoryId(personCategoryIndex))
            }.first()
        }
        return result
    }

    /**
     * Converts Advance Person Category Indexes to unique IDs for a given Advance
     * data source.  EX: LocationID 34100, Person Category Index 3 -> 341003
     */
    internal fun getExternalPersonCategoryId(personCategoryIndex : Int) : Int {
        return (10 * locationId) + personCategoryIndex
    }
    
    internal fun getDiscountName(label : String) : String {
        return label.removePrefix("Discount:").trim()
    }
    
    internal fun getDiscount(advanceDiscount : AdvancePriceAdjustment) : Discount {
        if(advanceDiscount.promotionID != null) {
            return Discount.find {
                (Discounts.externalSource eq source) and
                        (Discounts.externalId eq advanceDiscount.promotionID)
            }.first()
        }
        return Discount.find {
            (Discounts.externalSource eq source) and
                    (Discounts.name eq getDiscountName(advanceDiscount.label))
        }.first()
    }

    fun importCustomer(advanceCustomer : AdvanceCustomer) : Customer {
        return transaction {
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

            customer
        }
    }
    
    internal fun <T> api20Request(
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

    val currentUser : AdvanceUser? get() {
        return api20Request(
                "/authorize/current-user",
                AdvanceUserRespose::class.java
        ).user
    }
}