package ticketpile.service.controllers

import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.transactions.transaction
import ticketpile.service.ResourceNotFoundException
import ticketpile.service.model.*
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
                    ?: throw ResourceNotFoundException()
            if(adjustment.subject.id.value == subjectId) {
                adjustment.delete()
            }
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
                    ?: throw ResourceNotFoundException()
            if(adjustment.subject.id.value == subjectId) {
                adjustment.delete()
            }
            subject = subjectClass.findById(subjectId)
        }
        return subject!!
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
                    ?: throw ResourceNotFoundException()
            if(adjustment.subject.id.value == subjectId) {
                adjustment.delete()
            }
            subject = subjectClass.findById(subjectId)
        }
        return subject!!
    }
}