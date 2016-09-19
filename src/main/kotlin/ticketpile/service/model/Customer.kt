package ticketpile.service.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntityClass
import ticketpile.service.database.Customers
import ticketpile.service.util.PrimaryEntity

/**
 * Created by jonlatane on 9/19/16.
 */
class Customer(id: EntityID<Int>) : PrimaryEntity(id, Customers) {
    companion object : IntEntityClass<Customer>(Customers)
    /*internal val tickets : Iterable<Ticket>
            by children(Ticket, Tickets.booking)*/
    @get:JsonProperty
    var firstName by Customers.firstName
    @get:JsonProperty
    var lastName by Customers.lastName
    @get:JsonProperty
    var emailAddress by Customers.emailAddress
    @get:JsonProperty
    var address1 by Customers.address1
    @get:JsonProperty
    var address2 by Customers.address2
    @get:JsonProperty
    var state by Customers.state
    @get:JsonProperty
    var country by Customers.country

}