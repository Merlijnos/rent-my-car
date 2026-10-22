package rentmycar.auth

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import rentmycar.ErrorResponse
import rentmycar.startApp

class AuthRoutesTest {
    @Test
    fun `login returns a jwt token`() = testApplication {
        val http = startApp()

        val response =
            http.post("/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest("paul", "paul123"))
            }

        assertEquals(HttpStatusCode.OK, response.status)
        val token = response.body<TokenResponse>().token
        assertTrue(token.isNotBlank())
        assertTrue(token.count { it == '.' } == 2)
    }

    @Test
    fun `login with wrong password returns 401`() = testApplication {
        val http = startApp()

        val response =
            http.post("/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest("paul", "wrong"))
            }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals(ErrorResponse("Invalid username or password"), response.body<ErrorResponse>())
    }

    @Test
    fun `login with unknown user returns 401`() = testApplication {
        val http = startApp()

        val response =
            http.post("/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest("nobody", "paul123"))
            }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
