package ticketpile.service.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntityClass
import ticketpile.service.database.DiscountPersonCategories
import ticketpile.service.database.DiscountProducts
import ticketpile.service.database.Discounts
import ticketpile.service.model.basis.weighByApplicableGrossRevenue
import ticketpile.service.model.basis.weighByApplicableTicketCount
import ticketpile.service.model.transformation.Weighable
import ticketpile.service.util.PrimaryEntity
import ticketpile.service.util.RelationalEntity
import ticketpile.service.util.RelationalEntityClass
import java.math.BigDecimal

/**
 * Corresponds to the things about Advance Discounts necessary to
 * calculate ticket values accurately.
 * Created by jonlatane on 10/7/16.
 */
class Discount(id: EntityID<Int>) : PrimaryEntity(id, Discounts) {
    companion object : IntEntityClass<Discount>(Discounts)

    @get:JsonProperty
    var name: String by Discounts.name
    @get:JsonProperty
    var description: String by Discounts.description
    @get:JsonProperty
    val discountId: Int by PK
    @get:JsonProperty
    var basis: DiscountBasis by Discounts.basis

    private val _products by DiscountProduct childrenOn DiscountProducts.parent
    private val _personCategories by DiscountPersonCategory childrenOn DiscountPersonCategories.parent

    /**
     * Corresponds to Advance Product Associations
     */
    val products: Iterable<Product> get() {
        val result = mutableListOf<Product>()
        _products.forEach { result.add(it.product) }
        return result
    }
    /**
     * Corresponds to the Person Categories set on Advance Discounts.
     */
    val personCategories: Iterable<PersonCategory> get() {
        val result = mutableListOf<PersonCategory>()
        _personCategories.forEach { result.add(it.personCategory) }
        return result
    }
}

/**
 * How a discount should apply to tickets.  Enum names correspond to responses from Advance API.
 */
enum class DiscountBasis(val weightMethod : (BigDecimal, Weighable, Ticket, (Ticket) -> Boolean) -> BigDecimal) {
    PERPERSON(weighByApplicableTicketCount),
    PERBOOKING(weighByApplicableGrossRevenue)
}

/**
 * Fundamental component of discount applicability along with [DiscountProduct].
 */
class DiscountPersonCategory(id: EntityID<Int>) : RelationalEntity(id) {
    companion object : RelationalEntityClass<DiscountPersonCategory>(DiscountPersonCategories)
    var discount by Discount referencedOn DiscountPersonCategories.parent
    var personCategory by PersonCategory referencedOn DiscountPersonCategories.personCategory
}

/**
 * Fundamental component of discount applicability along with [DiscountPersonCategory].
 */
class DiscountProduct(id: EntityID<Int>) : RelationalEntity(id) {
    companion object : RelationalEntityClass<DiscountProduct>(DiscountProducts)
    var discount by Discount referencedOn DiscountProducts.parent
    var product by Product referencedOn DiscountProducts.product
}
