package rentmycar.users

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import rentmycar.ErrorResponse
import rentmycar.login
import rentmycar.startApp

class UserRoutesTest {
    @Test
    fun `register creates an owner that can log in`() = testApplication {
        val http = startApp()

        val response =
            http.post("/users/register") {
                contentType(ContentType.Application.Json)
                setBody(RegisterRequest("marie", "marie123", "marie@rentmycar.nl", Role.OWNER))
            }

        assertEquals(HttpStatusCode.Created, response.status)
        val created = response.body<UserResponse>()
        assertEquals("marie", created.username)
        assertEquals(Role.OWNER, created.role)

        val token = http.login("marie", "marie123")
        val me =
            http.get("/users/me") {
                header("Authorization", "Bearer $token")
            }
        assertEquals(HttpStatusCode.OK, me.status)
        assertEquals(created, me.body<UserResponse>())
    }

    @Test
    fun `register as renter stores the renter role`() = testApplication {
        val http = startApp()

        val response =
            http.post("/users/register") {
                contentType(ContentType.Application.Json)
                setBody(RegisterRequest("jan", "jan123", "jan@rentmycar.nl", Role.RENTER))
            }

        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals(Role.RENTER, response.body<UserResponse>().role)
    }

    @Test
    fun `duplicate username returns 400`() = testApplication {
        val http = startApp()

        val response =
            http.post("/users/register") {
                contentType(ContentType.Application.Json)
                setBody(RegisterRequest("paul", "other", "other@rentmycar.nl", Role.OWNER))
            }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals(ErrorResponse("Username 'paul' is already taken"), response.body<ErrorResponse>())
    }

    @Test
    fun `me without token returns 401`() = testApplication {
        val http = startApp()
        assertEquals(HttpStatusCode.Unauthorized, http.get("/users/me").status)
    }
}
