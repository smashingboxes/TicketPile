package ticketpile.service.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntityClass
import ticketpile.service.database.*
import ticketpile.service.util.PrimaryEntity
import ticketpile.service.util.RelationalEntity

/**
 * Created by jonlatane on 8/28/16.
 */
class Ticket(id: EntityID<Int>) : PrimaryEntity(id, Tickets) {
    companion object : IntEntityClass<Ticket>(Tickets)
    var bookingItem by BookingItem referencedOn Tickets.parent
    
    @get:JsonProperty
    val ticketId by PK
    @get:JsonProperty
    var code by Tickets.code
    @get:JsonProperty
    var personCategory by PersonCategory referencedOn Tickets.personCategory
    @get:JsonProperty
    var basePrice by Tickets.basePrice
}

class PersonCategory(id: EntityID<Int>) : PrimaryEntity(id, PersonCategories) {
    companion object : IntEntityClass<PersonCategory>(PersonCategories)

    @get:JsonProperty
    val personCategoryId by PK
    @get:JsonProperty
    var name by PersonCategories.name
    @get:JsonProperty
    var description by PersonCategories.description
}


class TicketAddOn(id: EntityID<Int>) : RelationalEntity(id) {
    companion object : IntEntityClass<TicketAddOn>(TicketAddOns)
    var ticket by Ticket referencedOn TicketAddOns.parent
    
    @get:JsonProperty
    val ticketAddOnId by PK
    @get:JsonProperty
    val addOn by AddOn referencedOn TicketAddOns.addon
    @get:JsonProperty
    val amount by TicketAddOns.amount
}

class TicketDiscount(id: EntityID<Int>) : RelationalEntity(id), DiscountAdjustment<Ticket> {
    companion object : IntEntityClass<TicketDiscount>(TicketDiscounts)
    var ticket by Ticket referencedOn TicketDiscounts.parent
    
    @get:JsonProperty
    val ticketDiscountId by IDDelegate(this)
    @get:JsonProperty
    override var discount by Discount referencedOn TicketDiscounts.discount
    @get:JsonProperty
    override var amount by TicketDiscounts.amount
    override var subject by Ticket referencedOn TicketDiscounts.parent
}

class TicketManualAdjustment(id: EntityID<Int>) : RelationalEntity(id) {
    companion object : IntEntityClass<TicketManualAdjustment>(TicketManualAdjustments)
    var ticket by Ticket referencedOn TicketManualAdjustments.parent
    
    @get:JsonProperty
    val ticketManualAdjustmentId by IDDelegate(this)
    @get:JsonProperty
    val description by TicketManualAdjustments.description
    @get:JsonProperty
    val amount by TicketManualAdjustments.amount
}