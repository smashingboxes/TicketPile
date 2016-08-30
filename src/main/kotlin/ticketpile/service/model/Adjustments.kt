package ticketpile.service.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import ticketpile.service.database.*
import ticketpile.service.util.IDDelegateOn
import java.math.BigDecimal
import kotlin.reflect.KProperty


/**
 * Created by jonlatane on 8/28/16.
 */

class Product(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Product>(Products)
    @get:JsonProperty
    var name by Products.name
    @get:JsonProperty
    var description by Products.description
    @get:JsonProperty
    val productId by IDDelegateOn(this)
}

class AddOn(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<AddOn>(AddOns)
    @get:JsonProperty
    var name : String by AddOns.name
    @get:JsonProperty
    var product : Product? by Product optionalReferencedOn AddOns.product
    @get:JsonProperty
    val addOnId by IDDelegateOn(this)
}

class Discount(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Discount>(Discounts)
    @get:JsonProperty
    var name : String by Discounts.name
    @get:JsonProperty
    var description : String by Discounts.description
    @get:JsonProperty
    val discountId : Int by IDDelegateOn(this) //Discounts.id
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
