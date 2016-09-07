package ticketpile.service.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import ticketpile.service.database.AddOns
import ticketpile.service.database.Discounts
import ticketpile.service.database.Products
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
}

class Discount(id: EntityID<Int>) : RelationalEntity(id) {
    companion object : IntEntityClass<Discount>(Discounts)
    @get:JsonProperty
    var name : String by Discounts.name
    @get:JsonProperty
    var description : String by Discounts.description
    @get:JsonProperty
    val discountId : Int by PK
}

interface DiscountAdjustment<SubjectType : IntEntity> {
    var subject : SubjectType
    @get:JsonProperty
    var discount : Discount
    @get:JsonProperty
    var amount : BigDecimal
}

interface AddOnAdjustment<SubjectType : IntEntity> {
    var subject : SubjectType
    @get:JsonProperty
    var addOn : AddOn
    @get:JsonProperty
    var amount : BigDecimal
}

interface ManualAdjustment<SubjectType : IntEntity> {
    var subject : SubjectType
    @get:JsonProperty
    var description : String
    @get:JsonProperty
    var amount : BigDecimal
}
