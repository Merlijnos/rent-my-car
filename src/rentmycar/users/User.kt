package rentmycar.users

/**
 * A platform user. [Role.OWNER] offers cars for rent; [Role.RENTER] rents cars.
 * [id] is 0 until the user is stored.
 */
data class User(
    val username: String,
    val password: String,
    val email: String,
    val role: Role,
    val id: Int = 0,
) {
    init {
        require(username.isNotBlank() && username.length <= 50) { "username must be 1 to 50 characters" }
        require(password.isNotBlank() && password.length <= 100) { "password must be 1 to 100 characters" }
        require(email.isNotBlank() && email.length <= 100) { "email must be 1 to 100 characters" }
        require('@' in email) { "email must contain @" }
    }
}

/** Whether the user offers cars ([OWNER]) or rents them ([RENTER]). */
enum class Role {
    OWNER,
    RENTER,
}
