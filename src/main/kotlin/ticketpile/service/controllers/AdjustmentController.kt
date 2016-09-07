package ticketpile.service.controllers

import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.transactions.transaction
import ticketpile.service.model.*
import ticketpile.service.util.BadRequestException
import ticketpile.service.util.ResourceNotFoundException
import java.math.BigDecimal

/**
 * Created by jonlatane on 8/30/16.
 */
open abstract class AdjustmentController<SubjectType: IntEntity> (
        val subjectClass : EntityClass<Int, SubjectType>
) {
    internal fun <AdjustmentType, AdjustmentClass> applyDiscount(
            subjectId: Int,
            discountId: Int,
            discountAmount: BigDecimal,
            adjustmentClass: AdjustmentClass
    ): AdjustmentType
            where AdjustmentType : DiscountAdjustment<SubjectType>,
            AdjustmentClass : EntityClass<Int, AdjustmentType> {
        if(discountAmount > BigDecimal.ZERO) 
            throw BadRequestException("Amount is too high!")
        var subjectAdjustment : AdjustmentType? = null

        transaction {
            logger.addLogger(StdOutSqlLogger())
            subjectAdjustment = adjustmentClass.new {
                subject = subjectClass.findById(subjectId)!!
                discount = Discount.findById(discountId)!!
                amount = discountAmount
            }
        }
        return subjectAdjustment!!
    }

    internal fun <AdjustmentType, AdjustmentClass> deleteDiscount(
            subjectId: Int,
            adjustmentId: Int,
            adjustmentClass: AdjustmentClass
    ): SubjectType
            where AdjustmentType : DiscountAdjustment<SubjectType>,
    AdjustmentType : IntEntity,
    AdjustmentClass : EntityClass<Int, AdjustmentType> {
        var subject : SubjectType? = null
        transaction {
            val adjustment = adjustmentClass.findById(adjustmentId)
                    ?: throw ResourceNotFoundException("Couldn't find discount")
            if(adjustment.subject.id.value == subjectId) {
                adjustment.delete()
            } else throw BadRequestException("Parent ID must match!")
            subject = subjectClass.findById(subjectId)
        }
        return subject!!
    }

    internal fun <AdjustmentType, AdjustmentClass> applyAddOn(
            subjectId: Int,
            addOnId: Int,
            addOnAmount: BigDecimal,
            adjustmentClass: AdjustmentClass
    ) :AdjustmentType where AdjustmentType : AddOnAdjustment<SubjectType>,
    AdjustmentClass : EntityClass<Int, AdjustmentType> {
        var subjectAdjustment : AdjustmentType? = null
        transaction {
            logger.addLogger(StdOutSqlLogger())
            subjectAdjustment = adjustmentClass.new {
                subject = subjectClass.findById(subjectId)!!
                addOn = AddOn.findById(addOnId)!!
                amount = addOnAmount
            }
        }
        return subjectAdjustment!!
    }

    internal fun <AdjustmentType, AdjustmentClass> deleteAddOn(
            subjectId: Int,
            adjustmentId: Int,
            adjustmentClass: AdjustmentClass
    ): SubjectType
            where AdjustmentType : AddOnAdjustment<SubjectType>,
    AdjustmentType : IntEntity,
    AdjustmentClass : EntityClass<Int, AdjustmentType> {
        var subject : SubjectType? = null
        transaction {
            val adjustment = adjustmentClass.findById(adjustmentId)
                    ?: throw ResourceNotFoundException("Couldn't find addon")
            if(adjustment.subject.id.value == subjectId) {
                adjustment.delete()
            } else throw BadRequestException("Parent ID must match!")
            subject = subjectClass.findById(subjectId)
        }
        return subject!!
    }
    
    internal fun <AdjustmentType, AdjustmentClass> applyManualAdjustment(
            subjectId: Int,
            desc: String,
            adjustmentAmount: BigDecimal,
            adjustmentClass: AdjustmentClass
    ) :AdjustmentType where AdjustmentType : ManualAdjustment<SubjectType>,
    AdjustmentClass : EntityClass<Int, AdjustmentType> {
        var subjectAdjustment : AdjustmentType? = null
        transaction {
            logger.addLogger(StdOutSqlLogger())
            subjectAdjustment = adjustmentClass.new {
                subject = subjectClass.findById(subjectId)!!
                description = desc
                amount = adjustmentAmount
            }
        }
        return subjectAdjustment!!
    }
    
    internal fun <AdjustmentType, AdjustmentClass> deleteManualAdjustment(
            subjectId: Int,
            adjustmentId: Int,
            adjustmentClass: AdjustmentClass
    ): SubjectType
            where AdjustmentType : ManualAdjustment<SubjectType>,
    AdjustmentType : IntEntity,
    AdjustmentClass : EntityClass<Int, AdjustmentType> {
        var subject : SubjectType? = null
        transaction {
            val adjustment = adjustmentClass.findById(adjustmentId)
                    ?: throw ResourceNotFoundException("Couldn't find manual adjustment")
            if(adjustment.subject.id.value == subjectId) {
                adjustment.delete()
            } else throw BadRequestException("Parent ID must match!")
            subject = subjectClass.findById(subjectId)
        }
        return subject!!
    }

    /*open internal fun applyEventAddOn(
            bookingId: Int,
            eventId: Int,
            addOnId: Int,
            addOnAmount: BigDecimal
    ): EventAddOn {
        var eventAddOn : EventAddOn? = null
        transaction {
            logger.addLogger(StdOutSqlLogger())
            eventAddOn = EventAddOn.new {
                booking = Booking.findById(bookingId)!!
                event = Event.findById(eventId)!!
                addOn = AddOn.findById(addOnId)!!
                amount = addOnAmount
            }
        }
        return eventAddOn!!
    }

    open internal fun deleteEventAddOn(
            bookingId: Int?,
            eventId: Int?,
            eventAddOnId: Int
    ): BookingEvent? {
        var bookingEvent: BookingEvent? = null
        transaction {
            val adjustment = EventAddOn.findById(eventAddOnId)
                    ?: throw ResourceNotFoundException("Addon not found")
            if(adjustment.booking.bookingId == bookingId
                    || adjustment.event.eventId == eventId) {
                adjustment.delete()
                bookingEvent = Booking.findById(adjustment.booking.id.value)!!
                        .events.filter {
                    it.event.eventId == adjustment.event.eventId
                }.first()
            } else throw BadRequestException("Booking or Event ID must match!")
        }
        return bookingEvent
    }*/
}