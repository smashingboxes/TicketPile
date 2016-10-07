package ticketpile.service.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import ticketpile.service.database.AddOns
import ticketpile.service.database.Products
import ticketpile.service.util.PrimaryEntity
import ticketpile.service.util.RelationalEntity
import java.math.BigDecimal


/**
 * Created by jonlatane on 8/28/16.
 */

interface Adjustment<SubjectType : IntEntity> {
    @get:JsonProperty
    var amount : BigDecimal
}

// This interface should be implemented for adjustments that come from
// mapping Booking/BookingItem adjustments to Tickets.
interface MappedAdjustment<out Source : Adjustment<*>> {
    val sourceAdjustment : Source
    val subject : Ticket
}

interface DiscountAdjustment<SubjectType : IntEntity> : Adjustment<SubjectType> {
    var subject : SubjectType
    @get:JsonProperty
    var discount : Discount
}

interface AddOnAdjustment<SubjectType : IntEntity> : Adjustment<SubjectType> {
    var subject : SubjectType
    @get:JsonProperty
    var addOn : AddOn
    @get:JsonProperty
    var prompt : String
}

interface ManualAdjustment<SubjectType : IntEntity> : Adjustment<SubjectType> {
    var subject : SubjectType
    @get:JsonProperty
    var description : String
}
