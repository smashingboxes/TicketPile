package ticketpile.service.security

import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.sql.SchemaUtils
import ticketpile.service.database.ReferenceTable
import ticketpile.service.database.RelationalTable
import ticketpile.service.util.RelationalEntity
import java.math.BigInteger
import java.security.SecureRandom

/**
 * Created by jonlatane on 9/3/16.
 */

internal val authKeySize = 255

object Users : RelationalTable("user") {
    val name = varchar("name", length = 128)
            .index(isUnique = true)
}


object AuthKeys : ReferenceTable("advanceAuthKey", Users) {
    val authKey = varchar("authKey", length = authKeySize)
            .index(isUnique = true)
    val expirationDate = datetime("expirationDate").nullable()
}

class User(id: EntityID<Int>) : RelationalEntity(id) {
    companion object : IntEntityClass<User>(Users)
    
    @get:JsonProperty
    val userId by PK
    @get:JsonProperty
    var name by Users.name
    
    val authKeys by children(AuthKey)
}

class AuthKey(id: EntityID<Int>) : RelationalEntity(id) {
    companion object : IntEntityClass<AuthKey>(AuthKeys)
    var user by User referencedOn AuthKeys.parent
    var authKey by AuthKeys.authKey
    var expirationDate by AuthKeys.expirationDate
}

internal val tables = arrayOf(
        Users, AuthKeys
)

fun initializeSecurity() {
    SchemaUtils.createMissingTablesAndColumns(*tables)
    val adminUser = User.all().firstOrNull() ?:
            User.new {
                name = "admin"
            }
    val adminAuthKey = adminUser.authKeys.firstOrNull() ?:
            AuthKey.new {
                user = adminUser
                expirationDate = null
                authKey = {
                    val random = SecureRandom()
                    BigInteger(5 * authKeySize, random).toString(32)
                }()
            }
    println("Security setup complete!")
    println("Admin auth token: ${adminAuthKey.authKey}")
}