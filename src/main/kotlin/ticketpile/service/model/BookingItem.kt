package ticketpile.service.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.sql.deleteWhere
import ticketpile.service.database.BookingItemAddOns
import ticketpile.service.database.BookingItems
import ticketpile.service.database.Tickets
import ticketpile.service.util.PrimaryEntity
import ticketpile.service.util.RelationalEntity
import ticketpile.service.util.RelationalEntityClass

/**
 * Created by jonlatane on 8/28/16.
 */

class BookingItem(id: EntityID<Int>) : PrimaryEntity(id, BookingItems), Weighable {
    companion object : RelationalEntityClass<BookingItem>(BookingItems)
    
    var booking by Booking referencedOn BookingItems.booking
    
    @get:JsonProperty
    var event by Event referencedOn BookingItems.event
    
    @get:JsonProperty
    val bookingItemId by PK
    @get:JsonProperty
    val addOns by BookingItemAddOn childrenOn BookingItemAddOns.parent
    @get:JsonProperty
    override val tickets by Ticket childrenOn Tickets.parent
    
    override fun delete() {
        tickets.forEach(Ticket::delete)
        BookingItemAddOns.deleteWhere {
            BookingItemAddOns.parent eq id
        }
        super.delete()
    }
}

class BookingItemAddOn(id: EntityID<Int>) : RelationalEntity(id), AddOnAdjustment<BookingItem> {
    companion object : RelationalEntityClass<BookingItemAddOn>(BookingItemAddOns)

    @get:JsonProperty
    val bookingAddOnId by PK
    @get:JsonProperty
    override var addOn by AddOn referencedOn BookingItemAddOns.addon
    @get:JsonProperty
    override var amount by BookingItemAddOns.amount
    @get:JsonProperty
    override var selection by BookingItemAddOns.selection

    override var subject by BookingItem referencedOn BookingItemAddOns.parent
}
