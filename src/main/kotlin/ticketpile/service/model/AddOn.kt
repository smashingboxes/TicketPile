package ticketpile.service.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntityClass
import ticketpile.service.database.AddOns
import ticketpile.service.util.RelationalEntity

/**
 * Created by jonlatane on 10/7/16.
 */
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