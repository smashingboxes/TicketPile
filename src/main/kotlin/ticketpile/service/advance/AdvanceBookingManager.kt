package ticketpile.service.advance

import org.jetbrains.exposed.sql.and
import ticketpile.service.database.*
import ticketpile.service.model.*
import ticketpile.service.util.BigZero
import ticketpile.service.util.decimalScale
import ticketpile.service.util.flushEntityCache
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * [AdvanceManager] responsible for importing booking data only.
 * 
 * Created by jonlatane on 10/10/16.
 */
class AdvanceBookingManager(host: String, authorizationKey: String, locationId: Int) :
        AdvanceManager(host, authorizationKey, locationId)
{

    fun getAdvanceBooking(reservationId: Int) : AdvanceReservation {
        return api20Request(
                "/bookings/$reservationId",
                AdvanceReservationResponse::class.java).booking
    }

    /**
     * Imports an [AdvanceReservation].  Assumes it shouldn't have to make any extra requests
     * and can be performed in a single transaction.  This means  
     * [AdvanceLocationManager.importRelatedAddOns], 
     * [AdvanceLocationManager.importRelatedDiscounts], and 
     * [AdvanceLocationManager.importRelatedAvailabilities] should have been called before
     * this for the same [reservation].
     */
    fun importByAdvanceReservation(reservation:AdvanceReservation) : Booking {

        // Delete the booking if it already exists.
        Booking.find {
            (Bookings.externalSource eq source) and
                    (Bookings.externalId eq reservation.bookingID)
        }.firstOrNull()?.delete()

        // Base import
        val bookingCustomer = importCustomer(reservation.customer)
        val booking = Booking.new {
            code = reservation.bookingCode
            externalId = reservation.bookingID
            externalSource = source
            location = locationId
            customer = bookingCustomer
            status = reservation.bookingStatus
        }

        importBookingItems(booking, reservation)
        importDiscounts(booking, reservation)
        importBookingAddOns(booking, reservation)
        importManualAdjustments(booking, reservation)
        importFees(booking, reservation)
        flushEntityCache()

        // Transformation of adjustments onto tickets
        TicketAdjustmentTransform.transform(booking)
        flushEntityCache()

        // Validation of import
        validateImportResult(booking, reservation)
        flushEntityCache()
        booking.matchesExternal = booking.errors.isEmpty()
        printValidationResponse(booking)
        
        // Ensure the booking cache is filled
        booking.populateCaches()
        
        return booking
    }
    
    private fun printValidationResponse(booking : Booking) {
        if(!booking.matchesExternal) {
            println("Validation errors for ${booking.code} (${booking.externalId}): " +
                    booking.errors.map {
                        "\n   ${it.errorType.description}: ${it.message}"
                    }.reduce {string1, string2 -> string1 + string2}
            )
        } else {
            println("Validation success for ${booking.code} (${booking.externalId})!")
        }
        if(booking.warnings.isNotEmpty()) {
            println("Validation warnings for ${booking.code} (${booking.externalId}): " +
                    booking.warnings.map {
                        "\n   ${it.errorType.description}: ${it.message}"
                    }.reduce {string1, string2 -> string1 + string2}
            )
        }
    }

    private fun importDiscounts(
            targetBooking: Booking,
            reservation: AdvanceReservation
    ) {
        // Advance tries to break down discounts on its own.
        // Undo that work, TicketPile is designed to do this better.
        val groupedDiscounts = reservation.pricing.priceAdjustments
                .filter { it.type == 1500 }.groupBy { it.promotionID }.values.map {
            it.reduce { priceAdjustment1, priceAdjustment2 ->
                AdvanceSyncError.new {
                    errorType = SyncErrorType.extraDiscountCodes
                    booking = targetBooking
                    message = "Advance PromotionId ${priceAdjustment1.promotionID} was listed more than once"
                }
                AdvancePriceAdjustment(
                        amount = priceAdjustment1.amount + priceAdjustment2.amount,
                        label = priceAdjustment1.label,
                        promotionID = priceAdjustment1.promotionID,
                        type = priceAdjustment1.type
                )
            }
        }

        groupedDiscounts.forEach{
            aDiscount ->
            val discountUsed = Discount.find {
                (Discounts.externalSource eq source) and
                        (Discounts.externalId eq aDiscount.promotionID)
            }.first()
            BookingDiscount.new {
                booking = targetBooking
                amount = aDiscount.amount
                discount = discountUsed
            }
        }
    }

    private fun importBookingAddOns(
            targetBooking: Booking,
            reservation: AdvanceReservation
    ) {
        reservation.addonSelections.forEach {
            advanceAddOnSelection ->
            advanceAddOnSelection.options.filter {
                it.label != null
            }.forEach {
                option ->
                val theAddOn = AddOn.find {
                    (AddOns.externalSource eq source) and
                            (AddOns.externalId eq advanceAddOnSelection.addonID)
                }.first()
                BookingAddOn.new {
                    addOn = theAddOn
                    selection = option.label!!
                    amount = theAddOn.basis.priceMethod(option.price ?: BigZero, targetBooking)
                    subject = targetBooking
                }
            }
        }
    }

    private fun importManualAdjustments(
            targetBooking: Booking,
            reservation: AdvanceReservation
    ) {
        reservation.lineTotals.filter {
            it.type in arrayOf(2000,2100) && !it.label.contains("ovation fee", ignoreCase = true)
        }.forEach {
            BookingManualAdjustment.new {
                subject = targetBooking
                amount = it.price
                description = it.label
            }
        }
    }


    private fun importFees(
            targetBooking: Booking,
            reservation: AdvanceReservation
    ) {
        reservation.lineTotals.filter {
            it.type in arrayOf(8300) ||
                    (it.type in arrayOf(2000,2100) && it.label.contains("ovation fee", ignoreCase = true))
        }.forEach {
            BookingFee.new {
                subject = targetBooking
                amount = it.price
                description = it.label
            }
        }
    }
    
    private fun importBookingItems(
            targetBooking: Booking,
            reservation: AdvanceReservation
    ) {
        reservation.bookingItems.forEach {
            aBookingItem: AdvanceBookingItem ->
            val targetEvent = Event.find {
                (Events.externalSource eq source) and
                        (Events.externalId eq aBookingItem.availabilityID)
            }.first()
            val bookingItem = BookingItem.new {
                booking = targetBooking
                event = targetEvent
                externalSource = source
                externalId = aBookingItem.bookingItemID
            }
            importTickets(aBookingItem, bookingItem)
            flushEntityCache()
            importBookingItemAddOns(aBookingItem, bookingItem)
        }
    }

    private fun importBookingItemAddOns(
            aBookingItem: AdvanceBookingItem,
            bookingItem: BookingItem
    ) {
        aBookingItem.addonSelections.forEach {
            advanceAddOnSelection ->
            advanceAddOnSelection.options.filter {
                it.label != null
            }.forEach {
                option ->
                val theAddOn = AddOn.find {
                    (AddOns.externalSource eq source) and
                            (AddOns.externalId eq advanceAddOnSelection.addonID)
                }.first()
                BookingItemAddOn.new {
                    addOn = theAddOn
                    selection = option.label!!
                    amount = theAddOn.basis.priceMethod(option.price ?: BigZero, bookingItem)
                    subject = bookingItem
                }
            }
        }
    }

    private data class PersonCategoryTicketData(
            val ticketPrice: BigDecimal,
            val totalTickets: Int,
            var ticketsCreated: Int = 0
    )

    private fun importTickets(
            aBookingItem: AdvanceBookingItem,
            targetBookingItem: BookingItem
    ) {
        val ticketMetadata = generatePersonCategoryTicketData(aBookingItem)
        val erroredPersonCategories = mutableSetOf<PersonCategory>()
        aBookingItem.ticketCodes.forEach {
            aTicketCode ->
            val thePersonCategory = getPersonCategory(aTicketCode.personCategoryIndex)
            val ticketData = ticketMetadata[thePersonCategory]
            if(ticketData != null && ticketData.ticketsCreated < ticketData.totalTickets) {
                Ticket.new {
                    code = aTicketCode.code
                    bookingItem = targetBookingItem
                    basePrice = ticketData.ticketPrice
                    personCategory = thePersonCategory
                }
                ticketData.ticketsCreated++
            } else if(ticketData != null //there were extra ticket codes
                    && !erroredPersonCategories.contains(thePersonCategory)) {
                AdvanceSyncError.new {
                    errorType = SyncErrorType.extraTicketCodes
                    booking = targetBookingItem.booking
                    message = "Advance ticket code ${aTicketCode.code} was skipped because pricing " +
                            "data only listed ${ticketData.totalTickets} tickets"
                }
                erroredPersonCategories.add(thePersonCategory)
            } else if(!erroredPersonCategories.contains(thePersonCategory)){
                AdvanceSyncError.new {
                    errorType = SyncErrorType.mismatchTicketCodes
                    booking = targetBookingItem.booking
                    message = "Advance ticket code ${aTicketCode.code} was skipped because pricing " +
                            "data didn't list any ${thePersonCategory.name} tickets"
                }
            }
        }
        createMissingTickets(targetBookingItem, ticketMetadata)
    }

    private fun generatePersonCategoryTicketData(
            aBookingItem: AdvanceBookingItem
    ) : Map<PersonCategory, PersonCategoryTicketData> {
        val result = mutableMapOf<PersonCategory, PersonCategoryTicketData>()
        aBookingItem.lineTotals.filter { it.type == 1101 }.forEach {
            lineTotal ->
            val personCategory = PersonCategory.find {
                (PersonCategories.externalSource eq source) and (PersonCategories.name eq lineTotal.label)
            }.first()
            val ticketPrice = lineTotal.price.setScale(decimalScale).divide(
                    BigDecimal(lineTotal.quantity).setScale(decimalScale),
                    RoundingMode.HALF_UP
            )
            result[personCategory] = PersonCategoryTicketData(ticketPrice, lineTotal.quantity)
        }
        return result
    }

    private fun createMissingTickets(
            targetBookingItem: BookingItem,
            ticketMetadata: Map<PersonCategory, PersonCategoryTicketData>
    ) {
        ticketMetadata.forEach {
            val thePersonCategory = it.key
            val ticketData = it.value
            var hasErrored = false
            while(ticketData.ticketsCreated < ticketData.totalTickets) {
                if(!hasErrored) {
                    AdvanceSyncError.new {
                        errorType = SyncErrorType.missingTicketCodes
                        booking = targetBookingItem.booking
                        message = "Created missing ticket code for ${thePersonCategory.name} ticket on Advance " +
                                "booking item ${targetBookingItem.externalId}"
                    }
                    hasErrored = true
                }
                Ticket.new {
                    code = "No Ticket Code Generated"
                    bookingItem = targetBookingItem
                    basePrice = ticketData.ticketPrice
                    personCategory = thePersonCategory
                }
                ticketData.ticketsCreated++
            }
        }
    }
}