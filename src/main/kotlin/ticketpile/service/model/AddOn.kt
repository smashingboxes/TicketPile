package ticketpile.service.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntityClass
import ticketpile.service.database.AddOns
import ticketpile.service.model.basis.pricePerItem
import ticketpile.service.model.basis.pricePerPerson
import ticketpile.service.model.transformation.Weighable
import ticketpile.service.model.basis.weighByApplicableGrossRevenue
import ticketpile.service.model.basis.weighByApplicableTicketCount
import ticketpile.service.util.PrimaryEntity
import ticketpile.service.util.decimalScale
import java.math.BigDecimal

enum class AddOnBasis(
        val weightMethod : (BigDecimal, Weighable, Ticket, (Ticket) -> Boolean) -> BigDecimal,
        val priceMethod : (BigDecimal, Weighable) -> BigDecimal
) {
    PERPERSON(weighByApplicableTicketCount, pricePerPerson),
    PERPRODUCT(weighByApplicableGrossRevenue, pricePerItem),
    PERUNIT(weighByApplicableGrossRevenue, pricePerItem),
    PERBOOKING(weighByApplicableGrossRevenue, pricePerItem)
}
class AddOn(id: EntityID<Int>) : PrimaryEntity(id, AddOns) {
    companion object : IntEntityClass<AddOn>(AddOns)
    @get:JsonProperty
    var name by AddOns.name
    @get:JsonProperty
    var basis by AddOns.basis
    @get:JsonProperty
    val addOnId by PK
}