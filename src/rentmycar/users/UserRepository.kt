package rentmycar.users

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class UserRepository(private val database: Database) {
    fun findAll(): List<User> =
        transaction(database) {
            UserTable.selectAll().orderBy(UserTable.id).map { it.toUser() }
        }

    fun findById(id: Int): User? =
        transaction(database) {
            UserTable.selectAll().where { UserTable.id eq id }.singleOrNull()?.toUser()
        }

    fun findByUsername(username: String): User? =
        transaction(database) {
            UserTable.selectAll().where { UserTable.username eq username }.singleOrNull()?.toUser()
        }

    fun create(user: User): User =
        transaction(database) {
            val id = UserTable.insertAndGetId { it.setValues(user) }
            UserTable.selectAll().where { UserTable.id eq id }.single().toUser()
        }

    fun update(user: User): Boolean =
        transaction(database) {
            UserTable.update({ UserTable.id eq user.id }) { it.setValues(user) } > 0
        }

    fun delete(id: Int): Boolean =
        transaction(database) {
            UserTable.deleteWhere { UserTable.id eq id } > 0
        }

    private fun ResultRow.toUser(): User =
        User(
            username = this[UserTable.username],
            password = this[UserTable.password],
            email = this[UserTable.email],
            role = this[UserTable.role],
            id = this[UserTable.id].value,
        )

    private fun UpdateBuilder<*>.setValues(user: User) {
        this[UserTable.username] = user.username
        this[UserTable.password] = user.password
        this[UserTable.email] = user.email
        this[UserTable.role] = user.role
    }
}
