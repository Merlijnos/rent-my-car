package rentmycar.users

import org.jetbrains.exposed.dao.id.IntIdTable

object UserTable : IntIdTable("users") {
    val username = varchar("username", 50).uniqueIndex()
    val password = varchar("password", 100)
    val email = varchar("email", 100)
    val role = enumerationByName<Role>("role", 10)
}
