package ticketpile.service.advance

import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.springframework.web.client.HttpClientErrorException
import ticketpile.service.database.*
import ticketpile.service.model.*
import ticketpile.service.util.transaction

/**
 * Handlers for per-location imports of Advance API20 bookings.
 * 
 * Created by jonlatane on 9/3/16.
 */

open class AdvanceLocationManager(host: String, authorizationKey: String, locationId: Int) : 
        AdvanceManager(host, authorizationKey, locationId) 
{
    val bookingManager = AdvanceBookingManager(host, authorizationKey, locationId)
    
    fun importRelatedAvailabilities(reservation : AdvanceReservation) {
        reservation.bookingItems.forEach {
            transaction {
                if(it.availabilityID != 0)
                    importAvailability(it.availabilityID)
                else
                    findAvailabilityFor(it)
            }
        }
    }

    private fun importAvailability(
            availabilityId: Int
    ): Event {
        val availability = api20Request(
                "/merchants/$locationId/calendars/$availabilityId",
                AdvanceAvailabilityResponse::class.java).calendarEntry
        val eventProduct = getProduct(availability.productID)
        val event = Event.find {
            (Events.externalSource eq source) and (Events.externalId eq availability.availabilityID)
        }.firstOrNull() ?: Event.new {
            externalId = availability.availabilityID
            externalSource = source
            startTime = availability.startDateTime
            endTime = availability.endDateTime
            capacity = availability.maxCapacity
            product = eventProduct
            locationId = this@AdvanceLocationManager.locationId 
        }
        event.startTime = availability.startDateTime
        event.endTime = availability.endDateTime
        event.capacity = availability.maxCapacity
        event.product = eventProduct
        event.locationId = this@AdvanceLocationManager.locationId
        return event
    }


    fun importRelatedDiscounts(reservation : AdvanceReservation) {
        reservation.pricing.priceAdjustments.filter{it.type == 1500}
                .forEach{
                    if(it.promotionID != null) {
                        importDiscount(it.promotionID!!)
                    } else {
                        importDiscount(it.label)
                    }
                }
    }
    private fun importDiscount(label : String) {
        val discountName = getDiscountName(label)
        transaction {
            Discount.find {
                (Discounts.externalSource eq source) and
                        (Discounts.externalId eq null as Int?) and
                        (Discounts.name eq discountName)
            }.firstOrNull() ?: Discount.new {
                externalSource = source
                externalId = null
                name = discountName
                description = ""
                basis = DiscountBasis.PERBOOKING
            }
        }
    }
    private fun importDiscount(
            promotionId: Int
    ) {
        val advancePromotion = try {
            api20Request(
                    "/promotions/$promotionId?promotionID=$promotionId",
                    AdvancePromotionResponse::class.java
            ).promotion
        } catch(t: HttpClientErrorException) {
            // Advance's lack of FKs strike again!
            AdvancePromotion(
                    calcbasis = "perbooking",
                    promotionCode = "Missing Advance Discount"
            )
        }
        val calcbasis = DiscountBasis.valueOf(advancePromotion.calcbasis.toUpperCase())
        transaction {
            val discount = Discount.find {
                (Discounts.externalSource eq source) and
                        (Discounts.externalId eq promotionId)
            }.firstOrNull() ?: Discount.new {
                externalSource = source
                externalId = promotionId
                name = advancePromotion.promotionCode
                description = advancePromotion.description ?: ""
                basis = calcbasis
            }
            discount.name = advancePromotion.promotionCode
            discount.description = advancePromotion.description ?: ""
            DiscountPersonCategories.deleteWhere {
                DiscountPersonCategories.parent eq discount.id
            }
            DiscountProducts.deleteWhere {
                DiscountProducts.parent eq discount.id
            }
            advancePromotion.associations.filter{
                it.productID != null
                        && it.selected == true
            }.forEach {
                association ->
                val associatedProduct = getProduct(association.productID!!)
                DiscountProduct.new {
                    this.discount = discount
                    product = associatedProduct
                }
            }
            advancePromotion.personCategories.filter {
                it.label != null
                        && it.selected == true
            }.forEach {
                category ->
                val personCategory = getPersonCategory(category.personCategoryIndex!!)
                DiscountPersonCategory.new {
                    this.discount = discount
                    this.personCategory = personCategory
                }
            }
        }
    }

    fun importRelatedAddOns(reservation : AdvanceReservation) {
        reservation.addonSelections.forEach {
            if(transaction {
                AddOn.find {
                    (AddOns.externalSource eq source) and
                            (AddOns.externalId eq it.addonID)
                }.firstOrNull()
            } == null)
                importAddOn(it)
        }
        reservation.bookingItems.forEach {
            it.addonSelections.forEach {
                if(transaction {
                    AddOn.find {
                        (AddOns.externalSource eq source) and
                                (AddOns.externalId eq it.addonID)
                    }.firstOrNull()
                } == null)
                    importAddOn(it)
            }
        }
    }

    private fun importAddOn(advanceAddOnSelection: AdvanceAddOnSelection) {
        val advanceAddOn = api20Request(
                "/merchants/$locationId/addons/${advanceAddOnSelection.addonID}",
                AdvanceAddOnResponse::class.java
        ).addon
        val priceBasis = AddOnBasis.valueOf(advanceAddOn.priceCalcBasis.toUpperCase())
        transaction {
            val addOnSelection = AddOn.find {
                (AddOns.externalSource eq source) and
                        (AddOns.externalId eq advanceAddOnSelection.addonID)
            }.firstOrNull() ?: AddOn.new {
                name = advanceAddOnSelection.name
                basis = priceBasis
                externalSource = source
                externalId = advanceAddOnSelection.addonID
            }
            addOnSelection.name = advanceAddOnSelection.name
            addOnSelection.basis = priceBasis
        }
    }
    
}