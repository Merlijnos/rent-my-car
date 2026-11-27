package nl.avans.rentmycar.feature.auth

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import nl.avans.rentmycar.core.network.apiCall
import nl.avans.rentmycar.core.session.AuthSession
import nl.avans.rentmycar.core.session.Session
import nl.avans.rentmycar.core.session.SessionUser

class AuthRepository(
    private val client: HttpClient,
    private val session: Session,
) {
    /** Logs in, then asks `/users/me` for the id and role, and stores both in the [Session]. */
    suspend fun login(username: String, password: String): Result<Unit> =
        apiCall {
            val token = client.post("auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(username, password))
            }.body<TokenResponse>().token

            // The session has no token yet, so this one call sets the header itself.
            val me = client.get("users/me") { bearerAuth(token) }.body<UserResponse>()

            session.signIn(AuthSession(token, SessionUser(me.id, me.username, me.role)))
        }
}
