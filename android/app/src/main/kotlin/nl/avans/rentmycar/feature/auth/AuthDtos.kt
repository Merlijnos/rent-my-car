package nl.avans.rentmycar.feature.auth

import kotlinx.serialization.Serializable
import nl.avans.rentmycar.core.session.Role

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class TokenResponse(val token: String)

@Serializable
data class UserResponse(
    val id: Int,
    val username: String,
    val email: String,
    val role: Role,
)
