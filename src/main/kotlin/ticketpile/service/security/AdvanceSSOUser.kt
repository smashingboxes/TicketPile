package ticketpile.service.security

import ticketpile.service.advance.AdvanceUser

/**
 * Created by jonlatane on 10/14/16.
 */
class AdvanceSSOUser(user : AdvanceUser) : ApplicationUser {
    override val administrator = false
    override val locations = user.merchants.map{it.locationId}

}