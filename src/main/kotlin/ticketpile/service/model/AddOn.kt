package ticketpile.service.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntityClass
import ticketpile.service.database.AddOns
import ticketpile.service.model.basis.pricePerItem
import ticketpile.service.model.basis.pricePerPerson
import ticketpile.service.model.basis.weighByApplicableGrossRevenue
import ticketpile.service.model.basis.weighByApplicableTicketCount
import ticketpile.service.model.transformation.Weighable
import ticketpile.service.util.PrimaryEntity
import java.math.BigDecimal

/**
 * Corresponds directly to an Advance Add-On.
 */
class AddOn(id: EntityID<Int>) : PrimaryEntity(id, AddOns) {
    companion object : IntEntityClass<AddOn>(AddOns)
    @get:JsonProperty
    var name by AddOns.name
    @get:JsonProperty
    var basis by AddOns.basis
    @get:JsonProperty
    val addOnId by PK
}

/**
 * TicketPile version of the calcbasis for AddOns in Advance.  Determines how an
 * Add-On adjustment's *apparent* value from Advance should actually be calculated.
 * 
 * Basically, we just need this for PERPERSON.  Advance JSON lists a $25/person adjustment
 * for 3 people as just a $25 adjustment.  This is used to see that the actual adjustment
 * was $75, and that when distributing the cost among tickets, it should use the appropriate
 * [weightMethod].
 */
enum class AddOnBasis(
        val weightMethod : (BigDecimal, Weighable, Ticket, (Ticket) -> Boolean) -> BigDecimal,
        val priceMethod : (BigDecimal, Weighable) -> BigDecimal
) {
    PERPERSON(weighByApplicableTicketCount, pricePerPerson),
    PERPRODUCT(weighByApplicableGrossRevenue, pricePerItem),
    PERUNIT(weighByApplicableGrossRevenue, pricePerItem),
    PERBOOKING(weighByApplicableGrossRevenue, pricePerItem)
}