package ticketpile.service.model

import com.fasterxml.jackson.annotation.JsonProperty
import ticketpile.service.util.BigZero
import ticketpile.service.util.decimalScale
import java.math.BigDecimal


/**
 * All the adjustments that can be accrued on stuff.
 * 
 * Right now Advance only supports:
 * 
 * - [DiscountAdjustment] on [Booking]: [BookingDiscount]
 * - [AddOnAdjustment] on [Booking]: [BookingAddOn]
 * - [ManualAdjustment] on [Booking]: [BookingManualAdjustment]
 * - [FeeAdjustment] on [Booking]: [BookingFee]
 * - [AddOnAdjustment] on [BookingItem]: [BookingItemAddOn]
 * 
 * A [MappedAdjustment] should always implement one of the above interfaces and
 * 
 * Created by jonlatane on 8/28/16.
 */
fun adjustmentTotal(adjustments : List<Adjustment<*>>) : BigDecimal {
    return adjustments.map{it.amount}.fold(
            initial = BigZero,
            operation = {
                amount1, amount2 ->
                amount1 + amount2
            }
    )
}

interface Adjustment<SubjectType : Weighable> {
    var subject : SubjectType
    @get:JsonProperty
    var amount : BigDecimal
}

// This interface should be implemented for adjustments that come from
// mapping Booking/BookingItem adjustments to Tickets.
interface MappedAdjustment<out Source : Adjustment<*>> : Adjustment<Ticket>{
    val sourceAdjustment : Source
    //val subject : Ticket
}

interface DiscountAdjustment<SubjectType : Weighable> : Adjustment<SubjectType> {
    @get:JsonProperty
    var discount : Discount
}

interface AddOnAdjustment<SubjectType : Weighable> : Adjustment<SubjectType> {
    @get:JsonProperty
    var addOn : AddOn
    @get:JsonProperty
    var selection: String
}

interface ManualAdjustment<SubjectType : Weighable> : Adjustment<SubjectType> {
    @get:JsonProperty
    var description : String
}

interface FeeAdjustment<SubjectType : Weighable> : Adjustment<SubjectType> {
    @get:JsonProperty
    var description : String
}