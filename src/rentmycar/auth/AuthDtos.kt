package rentmycar.auth

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
)

@Serializable
data class TokenResponse(
    val token: String,
)
