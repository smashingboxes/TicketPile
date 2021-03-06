package ticketpile.service.security

import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import ticketpile.service.advance.AdvanceSyncTasks
import ticketpile.service.util.ReferenceTable
import ticketpile.service.util.RelationalEntity
import ticketpile.service.util.RelationalTable
import ticketpile.service.util.transaction
import java.math.BigInteger
import java.security.SecureRandom

/**
 * Created by jonlatane on 9/3/16.
 */

internal val UserAuthKeysize = 255

object Users : RelationalTable("user") {
    val name = varchar("name", length = 128)
            .index(isUnique = true)
}


object UserAuthKeys : ReferenceTable("userAuthKey", Users) {
    val authKey = varchar("authKey", length = UserAuthKeysize)
            .index(isUnique = true)
    val expirationDate = datetime("expirationDate").nullable()
}

class User(id: EntityID<Int>) : RelationalEntity(id), ApplicationUser {
    companion object : IntEntityClass<User>(Users)
    
    @get:JsonProperty
    val userId by PK
    @get:JsonProperty
    var name by Users.name
    
    val authKeys by UserAuthKey referrersOn UserAuthKeys.parent
    
    override val administrator = true
    override val locations by lazy {
        transaction {
            AdvanceSyncTasks.slice(AdvanceSyncTasks.advanceLocationId)
                .selectAll().groupBy(AdvanceSyncTasks.advanceLocationId)
                .map{it[AdvanceSyncTasks.advanceLocationId]}
        }
    }
}

class UserAuthKey(id: EntityID<Int>) : RelationalEntity(id) {
    companion object : IntEntityClass<UserAuthKey>(UserAuthKeys)
    var user by User referencedOn UserAuthKeys.parent
    var authKey by UserAuthKeys.authKey
    var expirationDate by UserAuthKeys.expirationDate
}

internal val tables = arrayOf(
        Users, UserAuthKeys
)

fun initializeSecurity() {
    SchemaUtils.createMissingTablesAndColumns(*tables)
    val adminUser = User.all().firstOrNull() ?:
            User.new {
                name = "admin"
            }
    val adminAuthKey = adminUser.authKeys.firstOrNull() ?:
            UserAuthKey.new {
                user = adminUser
                expirationDate = null
                authKey = {
                    val random = SecureRandom()
                    BigInteger(5 * UserAuthKeysize, random).toString(32)
                }()
            }
    println("Security setup complete!")
    println("Admin auth token: ${adminAuthKey.authKey}")
}