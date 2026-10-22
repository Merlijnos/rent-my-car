package rentmycar.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date
import rentmycar.users.User
import rentmycar.users.UserRepository

class JwtService(
    private val config: JwtConfig,
    private val users: UserRepository,
) {
    private val algorithm = Algorithm.HMAC256(config.secret)

    val verifier =
        JWT
            .require(algorithm)
            .withAudience(config.audience)
            .withIssuer(config.issuer)
            .build()

    fun login(username: String, password: String): String? {
        val user = users.findByUsername(username) ?: return null
        if (user.password != password) return null
        return createToken(user)
    }

    fun createToken(user: User): String =
        JWT
            .create()
            .withAudience(config.audience)
            .withIssuer(config.issuer)
            .withClaim("userId", user.id)
            .withClaim("username", user.username)
            .withClaim("role", user.role.name)
            .withExpiresAt(Date(System.currentTimeMillis() + TOKEN_TTL_MS))
            .sign(algorithm)

    fun findUserFromClaims(userId: Int?): User? =
        userId?.let { users.findById(it) }

    val realm: String get() = config.realm
    val audience: String get() = config.audience

    companion object {
        private const val TOKEN_TTL_MS = 3_600_000L
    }
}
