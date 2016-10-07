package ticketpile.service.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntityClass
import ticketpile.service.database.Products
import ticketpile.service.util.PrimaryEntity

/**
 * Created by jonlatane on 10/7/16.
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