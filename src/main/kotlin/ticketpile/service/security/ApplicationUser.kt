package ticketpile.service.security

/**
 * Created by jonlatane on 10/14/16.
 */
interface ApplicationUser {
    val administrator : Boolean
    val locations : List<Int>
}