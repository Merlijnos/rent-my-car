package rentmycar

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import java.util.UUID
import rentmycar.auth.JwtConfig
import rentmycar.auth.LoginRequest
import rentmycar.auth.TokenResponse

val testJwtConfig =
    JwtConfig(
        secret = "test-secret",
        issuer = "http://test/",
        audience = "rent-my-car-test",
        realm = "Rent my car test",
    )

fun ApplicationTestBuilder.startApp(): HttpClient {
    application { module("jdbc:h2:mem:${UUID.randomUUID()};DB_CLOSE_DELAY=-1", testJwtConfig) }
    return createClient { install(ContentNegotiation) { json() } }
}

suspend fun HttpClient.login(username: String, password: String): String {
    val response =
        post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username, password))
        }
    return response.body<TokenResponse>().token
}
