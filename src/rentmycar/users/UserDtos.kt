package rentmycar.users

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val username: String,
    val password: String,
    val email: String,
    val role: Role,
) {
    fun toUser(): User = User(username, password, email, role)
}

@Serializable
data class UserResponse(
    val id: Int,
    val username: String,
    val email: String,
    val role: Role,
)

fun User.toResponse() =
    UserResponse(
        id = id,
        username = username,
        email = email,
        role = role,
    )
