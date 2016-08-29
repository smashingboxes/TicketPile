package ticketpile.service.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import ticketpile.service.database.*
import java.math.BigDecimal


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
    val productId = id.value
}

class AddOn(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<AddOn>(AddOns)
    @get:JsonProperty
    var name by AddOns.name
    @get:JsonProperty
    var product by Product optionalReferencedOn AddOns.product
    @get:JsonProperty
    val addOnId = id.value
}

class Discount(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Discount>(Discounts)
    @get:JsonProperty
    var name by Discounts.name
    @get:JsonProperty
    var description by Discounts.description
    @get:JsonProperty
    val discountId = id.value
}