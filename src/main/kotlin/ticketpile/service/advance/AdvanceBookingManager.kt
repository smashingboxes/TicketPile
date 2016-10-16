package ticketpile.service.advance

import org.jetbrains.exposed.sql.and
import ticketpile.service.database.AddOns
import ticketpile.service.database.Bookings
import ticketpile.service.database.Customers
import ticketpile.service.database.PersonCategories
import ticketpile.service.model.*
import ticketpile.service.model.transformation.TicketAdjustmentTransform
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

        val bookingCustomer = Customer.find {
            (Customers.externalSource eq source) and
                    (Customers.externalId eq reservation.customer.customerID)
        }.first()
        
        // Base import
        val booking = Booking.new {
            code = reservation.bookingCode
            externalId = reservation.bookingID
            externalSource = source
            location = locationId
            customer = bookingCustomer
            status = reservation.bookingStatus
            taxAmount = reservation.pricing.taxAmount ?: BigZero
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
                .filter { it.type == 1500 }.groupBy { it.promotionID to it.label }.values.map {
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
            val discountUsed = getDiscount(it)
            BookingDiscount.new {
                booking = targetBooking
                amount = it.amount
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
                val theAddOn = AddOn.find {
                    (AddOns.externalSource eq source) and
                            (AddOns.externalId eq advanceAddOnSelection.addonID)
                }.first()
                BookingAddOn.new {
                    addOn = theAddOn
                    selection = it.label!!
                    amount = theAddOn.basis.priceMethod(it.price ?: BigZero, targetBooking)
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
            val targetEvent = findAvailabilityFor(it)
            val bookingItem = BookingItem.new {
                booking = targetBooking
                event = targetEvent
                externalSource = source
                externalId = it.bookingItemID
            }
            flushEntityCache()
            importTickets(it, bookingItem)
            importBookingItemAddOns(it, bookingItem)
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
        val ticketMetadata = generatePersonCategoryTicketData(aBookingItem, targetBookingItem)
        val recycledTicketCodes = mutableListOf<AdvanceTicketCode>()
        aBookingItem.ticketCodes.forEach {
            //aTicketCode ->
            val thePersonCategory = getPersonCategory(it.personCategoryIndex)
            val ticketData = ticketMetadata[thePersonCategory]
            if(ticketData != null && ticketData.ticketsCreated < ticketData.totalTickets) {
                Ticket.new {
                    code = it.code
                    bookingItem = targetBookingItem
                    basePrice = ticketData.ticketPrice
                    personCategory = thePersonCategory
                }
                ticketData.ticketsCreated++
            } else if(ticketData != null ) {
                // There were extra ticket codes
                AdvanceSyncError.new {
                    errorType = SyncErrorType.extraTicketCodes
                    booking = targetBookingItem.booking
                    message = "Advance ticket code ${it.code} was skipped because pricing " +
                            "data only listed ${ticketData.totalTickets} tickets"
                }
                recycledTicketCodes.add(it)
            } else {
                // There were no references to this Person Category in pricing
                AdvanceSyncError.new {
                    errorType = SyncErrorType.mismatchTicketCodes
                    booking = targetBookingItem.booking
                    message = "Advance ticket code ${it.code} was skipped because pricing " +
                            "data didn't list any ${thePersonCategory.name} tickets"
                }
                recycledTicketCodes.add(it)
            }
        }
        createMissingTickets(targetBookingItem, ticketMetadata, recycledTicketCodes)
        recycledTicketCodes.forEach {
            AdvanceSyncError.new {
                errorType = SyncErrorType.unusableTicketCode
                booking = targetBookingItem.booking
                message = "Skipped ${getPersonCategory(it.personCategoryIndex)} ticket ${it.code} entirely."
            }
        }
    }

    private fun generatePersonCategoryTicketData(
            aBookingItem: AdvanceBookingItem,
            targetBookingItem: BookingItem
    ) : Map<PersonCategory, PersonCategoryTicketData> {
        val result = mutableMapOf<PersonCategory, PersonCategoryTicketData>()
        aBookingItem.lineTotals.filter { it.type == 1101 }.forEach {
            val personCategory = PersonCategory.find {
                (PersonCategories.externalSource eq source) and (PersonCategories.name eq it.label)
            }.firstOrNull() ?: PersonCategory.new {
                externalId = null
                externalSource = source
                name = it.label
                description = ""
            }
            if(personCategory.externalId == null) {
                //flushEntityCache()
                AdvanceSyncError.new {
                    errorType = SyncErrorType.missingPersonCategory
                    booking = targetBookingItem.booking
                    message = "Reference to missing Person Category ${personCategory.name}. TicketPile version has " +
                            "TicketPile ID {${personCategory.id.value} and no description."
                }
            }
            
            val ticketPrice = it.price.setScale(decimalScale).divide(
                    BigDecimal(it.quantity).setScale(decimalScale),
                    RoundingMode.HALF_UP
            )
            result[personCategory] = PersonCategoryTicketData(ticketPrice, it.quantity)
        }
        return result
    }

    private fun createMissingTickets(
            targetBookingItem: BookingItem,
            ticketMetadata: Map<PersonCategory, PersonCategoryTicketData>,
            recycledTicketCodes : MutableList<AdvanceTicketCode>
    ) {
        ticketMetadata.forEach {
            val thePersonCategory = it.key
            val ticketData = it.value
            while(ticketData.ticketsCreated < ticketData.totalTickets) {
                var code = "No Ticket Code Generated"
                if(recycledTicketCodes.isEmpty()) {
                    AdvanceSyncError.new {
                        errorType = SyncErrorType.missingTicketCodes
                        booking = targetBookingItem.booking
                        message = "Created missing ticket code for ${thePersonCategory.name} ticket on Advance " +
                                "booking item ${targetBookingItem.externalId}"
                    }
                } else {
                    val ticketCode = recycledTicketCodes.removeAt(0)
                    code = ticketCode.code
                    AdvanceSyncError.new {
                        errorType = SyncErrorType.missingTicketCodes
                        booking = targetBookingItem.booking
                        message = "Recycled skipped ${getPersonCategory(ticketCode.personCategoryIndex)} " +
                                "ticket code $code as as a {${thePersonCategory.name} ticket instead."
                    }
                }
                Ticket.new {
                    this.code = code
                    bookingItem = targetBookingItem
                    basePrice = ticketData.ticketPrice
                    personCategory = thePersonCategory
                }
                ticketData.ticketsCreated++
            }
        }
    }
}