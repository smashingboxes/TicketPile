package ticketpile.service.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntityClass
import ticketpile.service.database.AddOns
import ticketpile.service.util.RelationalEntity
import java.math.BigDecimal

/**
 * Created by jonlatane on 10/7/16.
 */
val pricePerItem = {
    amount: BigDecimal, weighable: Weighable ->
    amount
}
val pricePerPerson = {
    amount: BigDecimal, weighable: Weighable ->
    amount * (BigDecimal(weighable.tickets.count()).setScale(amount.scale()))
}
enum class AddOnBasis(
        val weightMethod : (BigDecimal, Weighable, Ticket, (Ticket) -> Boolean) -> BigDecimal,
        val priceMethod : (BigDecimal, Weighable) -> BigDecimal
) {
    PERPERSON(weighByApplicableTicketCount, pricePerPerson),
    PERPRODUCT(weighByApplicableGrossRevenue, pricePerItem),
    PERUNIT(weighByApplicableGrossRevenue, pricePerItem)
}
class AddOn(id: EntityID<Int>) : RelationalEntity(id) {
    companion object : IntEntityClass<AddOn>(AddOns)
    @get:JsonProperty
    var name by AddOns.name
    @get:JsonProperty
    var basis by AddOns.basis
    @get:JsonProperty
    val addOnId by PK
    var externalSource by AddOns.externalSource
    var externalId by AddOns.externalId
}