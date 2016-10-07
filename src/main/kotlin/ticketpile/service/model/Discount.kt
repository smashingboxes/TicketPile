package ticketpile.service.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntityClass
import ticketpile.service.database.DiscountPersonCategories
import ticketpile.service.database.DiscountProducts
import ticketpile.service.database.Discounts
import ticketpile.service.util.RelationalEntity
import java.math.BigDecimal

/**
 * Created by jonlatane on 10/7/16.
 */

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