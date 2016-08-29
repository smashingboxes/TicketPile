package ticketpile.service.model

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import ticketpile.service.database.*

/**
 * Created by jonlatane on 8/28/16.
 */
class Ticket(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Ticket>(Tickets)
    var booking by Booking referencedOn Tickets.booking
    var event by Event referencedOn Tickets.event
    var personCategory by PersonCategory referencedOn Tickets.personCategory
    var basePrice by Tickets.basePrice
}

class PersonCategory(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PersonCategory>(PersonCategories)
    var name by PersonCategories.name
    var description by PersonCategories.description
}


class TicketAddOn(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<TicketAddOn>(TicketAddOns)
    var ticket by TicketAddOns.parent
    val addOnSelection by AddOn referencedOn TicketAddOns.addon
}

class TicketDiscount(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<TicketDiscount>(TicketDiscounts)
    var ticket by TicketDiscounts.parent
    val discount by TicketDiscounts.discount
    val amount by TicketDiscounts.amount
}

class TicketManualAdjustment(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<TicketManualAdjustment>(TicketManualAdjustments)
    var ticket by TicketManualAdjustments.parent
    val description by TicketManualAdjustments.description
    val amount by TicketManualAdjustments.amount
}