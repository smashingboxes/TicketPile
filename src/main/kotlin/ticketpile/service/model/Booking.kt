package ticketpile.service.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.sql.deleteWhere
import ticketpile.service.advance.AdvanceSyncError
import ticketpile.service.advance.AdvanceSyncErrors
import ticketpile.service.advance.SyncErrorLevel
import ticketpile.service.database.*
import ticketpile.service.util.PrimaryEntity
import ticketpile.service.util.RelationalEntity
import ticketpile.service.util.RelationalEntityClass
import java.math.BigDecimal

/**
 * Corresponds to a Booking/Reservation in Advance.
 * 
 * Created by jonlatane on 8/28/16.
 */

class Booking(id: EntityID<Int>) : PrimaryEntity(id, Bookings), Weighable {
    companion object : IntEntityClass<Booking>(Bookings)

    @get:JsonProperty
    val bookingId by PK
    
    @get:JsonProperty 
    var code by Bookings.code
    
    @get:JsonProperty
    var status by Bookings.status
    
    @get:JsonProperty
    val items by BookingItem childrenOn BookingItems.booking
    
    @get:JsonProperty
    val addOns by BookingAddOn childrenOn BookingAddOns.parent
    
    @get:JsonProperty
    val discounts by BookingDiscount childrenOn BookingDiscounts.parent
    
    @get:JsonProperty
    val manualAdjustments by BookingManualAdjustment childrenOn BookingManualAdjustments.parent
    
    @get:JsonProperty
    val fees by BookingFee childrenOn BookingFees.parent
    
    @get:JsonProperty
    var customer by Customer referencedOn Bookings.customer
    
    @get:JsonProperty
    var location by Bookings.locationId
    
    @get:JsonProperty
    val bookingTotal :BigDecimal get() {
        return grossRevenue
    }
    
    val _errors by AdvanceSyncError childrenOn AdvanceSyncErrors.parent
    @get:JsonProperty
    val errors : List<AdvanceSyncError> get() {
        return _errors.filter {it.errorType.level == SyncErrorLevel.error}
    }
    @get:JsonProperty
    val warnings : List<AdvanceSyncError> get() {
        return _errors.filter {it.errorType.level == SyncErrorLevel.warning}
    }
    
    override val tickets : List<Ticket> get() {
        val result = mutableListOf<Ticket>()
        items.forEach {
            it.tickets.forEach { 
                result.add(it)
            }
        }
        return result
    }
    
    var matchesExternal by Bookings.matchesExternal
    
    override fun delete() {
        items.forEach(BookingItem::delete)
        BookingAddOns.deleteWhere {
            BookingAddOns.parent eq id
        }
        BookingDiscounts.deleteWhere {
            BookingDiscounts.parent eq id
        }
        BookingManualAdjustments.deleteWhere {
            BookingManualAdjustments.parent eq id
        }
        BookingFees.deleteWhere {
            BookingFees.parent eq id
        }
        AdvanceSyncErrors.deleteWhere { 
            AdvanceSyncErrors.parent eq id
        }
        super.delete()
    }
}

class BookingAddOn(id: EntityID<Int>) : RelationalEntity(id), AddOnAdjustment<Booking> {
    companion object : RelationalEntityClass<BookingAddOn>(BookingAddOns)
    var booking by Booking referencedOn BookingAddOns.parent

    @get:JsonProperty
    val bookingAddOnId by PK
    @get:JsonProperty
    override var addOn by AddOn referencedOn BookingAddOns.addon
    @get:JsonProperty
    override var amount by BookingAddOns.amount
    @get:JsonProperty
    override var selection by BookingAddOns.selection
    
    override var subject by Booking referencedOn BookingAddOns.parent
}

class BookingDiscount(id: EntityID<Int>) : RelationalEntity(id), DiscountAdjustment<Booking> {
    companion object : RelationalEntityClass<BookingDiscount>(BookingDiscounts)
    internal var booking by Booking referencedOn BookingDiscounts.parent

    @get:JsonProperty
    val bookingDiscountId by PK
    @get:JsonProperty
    override var discount by Discount referencedOn BookingDiscounts.discount
    @get:JsonProperty
    override var amount by BookingDiscounts.amount
    
    override var subject by Booking referencedOn BookingDiscounts.parent
}

class BookingManualAdjustment(id: EntityID<Int>) : RelationalEntity(id), ManualAdjustment<Booking> {
    companion object : RelationalEntityClass<BookingManualAdjustment>(BookingManualAdjustments)
    var booking by Booking referencedOn BookingManualAdjustments.parent

    @get:JsonProperty
    val bookingManualAdjustmentId by PK
    @get:JsonProperty
    override var description by BookingManualAdjustments.description
    @get:JsonProperty
    override var amount by BookingManualAdjustments.amount

    override var subject by Booking referencedOn BookingManualAdjustments.parent
}

class BookingFee(id: EntityID<Int>) : RelationalEntity(id), FeeAdjustment<Booking> {
    companion object : RelationalEntityClass<BookingFee>(BookingFees)
    var booking by Booking referencedOn BookingFees.parent

    @get:JsonProperty
    val bookingFeeId by PK
    @get:JsonProperty
    override var description by BookingFees.description
    @get:JsonProperty
    override var amount by BookingFees.amount
    override var subject by Booking referencedOn BookingFees.parent
}