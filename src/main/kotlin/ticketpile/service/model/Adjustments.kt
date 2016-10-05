package ticketpile.service.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import ticketpile.service.database.*
import ticketpile.service.util.PrimaryEntity
import ticketpile.service.util.RelationalEntity
import java.math.BigDecimal


/**
 * Created by jonlatane on 8/28/16.
 */

class Product(id: EntityID<Int>) : PrimaryEntity(id, Products) {
    companion object : IntEntityClass<Product>(Products)
    @get:JsonProperty
    var name by Products.name
    @get:JsonProperty
    var description by Products.description
    @get:JsonProperty
    val productId by PK
}

class AddOn(id: EntityID<Int>) : RelationalEntity(id) {
    companion object : IntEntityClass<AddOn>(AddOns)
    @get:JsonProperty
    var name : String by AddOns.name
    @get:JsonProperty
    var product : Product? by Product optionalReferencedOn AddOns.product
    @get:JsonProperty
    val addOnId by PK
    var externalSource by AddOns.externalSource
    var externalId by AddOns.externalId
}

enum class DiscountBasis(val weightMethod : (BigDecimal, Weighable, Ticket, (Ticket) -> Boolean) -> BigDecimal) {
    PERPERSON(weighByApplicableTicketCount),
    PERBOOKING(weighByApplicableGrossRevenue)
}

class DiscountPersonCategory(id: EntityID<Int>) : RelationalEntity(id) {
    companion object : IntEntityClass<DiscountPersonCategory>(DiscountPersonCategories)
    var discount by Discount referencedOn DiscountPersonCategories.parent
    var personCategory by PersonCategory referencedOn DiscountPersonCategories.personCategory
}

class DiscountProduct(id: EntityID<Int>) : RelationalEntity(id) {
    companion object : IntEntityClass<DiscountProduct>(DiscountProducts)
    var discount by Discount referencedOn DiscountProducts.parent
    var product by Product referencedOn DiscountProducts.product
}

class Discount(id: EntityID<Int>) : RelationalEntity(id) {
    companion object : IntEntityClass<Discount>(Discounts)
    @get:JsonProperty
    var name : String by Discounts.name
    @get:JsonProperty
    var description : String by Discounts.description
    @get:JsonProperty
    val discountId : Int by PK
    @get:JsonProperty
    var basis : DiscountBasis by Discounts.basis
    
    private val _products by DiscountProduct referrersOn DiscountProducts.parent
    private val _personCategories by DiscountPersonCategory referrersOn DiscountPersonCategories.parent
    val products : Iterable<Product> by lazy {
        val result = mutableListOf<Product>()
        _products.forEach { result.add(it.product) }
        result
    }
    val personCategories : Iterable<PersonCategory> by lazy {
        val result = mutableListOf<PersonCategory>()
        _personCategories.forEach { result.add(it.personCategory) }
        result
    }
    
    var externalSource by Discounts.externalSource
    var externalId by Discounts.externalId
}

interface Adjustment<SubjectType : IntEntity> {
    @get:JsonProperty
    var amount : BigDecimal
}

// This interface should be implemented for adjustments that come from
// mapping Booking/BookingItem adjustments to Tickets.
interface MappedAdjustment<out Source : Adjustment<*>> {
    val sourceAdjustment : Source
    val subject : Ticket
}

interface DiscountAdjustment<SubjectType : IntEntity> : Adjustment<SubjectType> {
    var subject : SubjectType
    @get:JsonProperty
    var discount : Discount
}

interface AddOnAdjustment<SubjectType : IntEntity> : Adjustment<SubjectType> {
    var subject : SubjectType
    @get:JsonProperty
    var addOn : AddOn
    @get:JsonProperty
    var prompt : String
}

interface ManualAdjustment<SubjectType : IntEntity> : Adjustment<SubjectType> {
    var subject : SubjectType
    @get:JsonProperty
    var description : String
}
