package ticketpile.service.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntityClass
import ticketpile.service.database.BookingItemAddOns
import ticketpile.service.database.BookingItems
import ticketpile.service.util.PrimaryEntity
import ticketpile.service.util.RelationalEntity

/**
 * Created by jonlatane on 8/28/16.
 */

class BookingItem(id: EntityID<Int>) : PrimaryEntity(id, BookingItems), Weighable {
    companion object : IntEntityClass<BookingItem>(BookingItems)
    var booking by Booking referencedOn BookingItems.booking
    var event by Event referencedOn BookingItems.event
    
    @get:JsonProperty
    val bookingItemId by PK
    @get:JsonProperty
    val addOns by children(BookingItemAddOn)
    @get:JsonProperty
    override val tickets : Iterable<Ticket> by children(Ticket)
}

class BookingItemAddOn(id: EntityID<Int>) : RelationalEntity(id), AddOnAdjustment<BookingItem> {
    companion object : IntEntityClass<BookingItemAddOn>(BookingItemAddOns)

    @get:JsonProperty
    val bookingAddOnId by PK
    @get:JsonProperty
    override var addOn by AddOn referencedOn BookingItemAddOns.addon
    @get:JsonProperty
    override var amount by BookingItemAddOns.amount
    @get:JsonProperty
    override var prompt by BookingItemAddOns.prompt

    override var subject by BookingItem referencedOn BookingItemAddOns.parent
}
