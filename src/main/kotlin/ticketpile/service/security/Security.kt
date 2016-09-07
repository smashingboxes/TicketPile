package ticketpile.service.security

import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntityClass
import ticketpile.service.database.ReferenceTable
import ticketpile.service.database.RelationalTable
import ticketpile.service.util.RelationalEntity

/**
 * Created by jonlatane on 9/3/16.
 */

object Users : RelationalTable("user") {
    val name = varchar("name", length = 128)
            .index(isUnique = true)
}


object AuthKeys : ReferenceTable("authKey", Users) {
    val key = varchar("key", length = 512)
            .index(isUnique = true)
    val creationDate = datetime("creationDate")
}

class User(id: EntityID<Int>) : RelationalEntity(id) {
    companion object : IntEntityClass<User>(Users)
    
    @get:JsonProperty
    val userId by PK
    @get:JsonProperty
    val name by Users.name
    
    //val authKeys by children(AuthKey)
}

class AuthKey(id: EntityID<Int>) : RelationalEntity(id) {
    companion object : IntEntityClass<AuthKey>(AuthKeys)
    
    val key by AuthKeys.key
}