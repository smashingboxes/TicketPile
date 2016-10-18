package ticketpile.service.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntityClass
import ticketpile.service.database.PersonCategories
import ticketpile.service.util.PrimaryEntity

/**
 * Corresponds to an Advance Person Category, but TicketPile supports as many as
 * we want rather than just 8.
 * 
 * Created by jonlatane on 10/17/16.
 */
class PersonCategory(id: EntityID<Int>) : PrimaryEntity(id, PersonCategories) {
    companion object : IntEntityClass<PersonCategory>(PersonCategories)

    @get:JsonProperty
    val personCategoryId by PK
    @get:JsonProperty
    var name by PersonCategories.name
    @get:JsonProperty
    var description by PersonCategories.description
}