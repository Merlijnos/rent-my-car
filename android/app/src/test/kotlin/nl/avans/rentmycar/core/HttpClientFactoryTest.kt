package nl.avans.rentmycar.core

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import nl.avans.rentmycar.core.network.ApiException
import nl.avans.rentmycar.core.network.createHttpClient
import nl.avans.rentmycar.core.session.AuthSession
import nl.avans.rentmycar.core.session.Role
import nl.avans.rentmycar.core.session.Session
import nl.avans.rentmycar.core.session.SessionState
import nl.avans.rentmycar.core.session.SessionUser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HttpClientFactoryTest {
    private val paulSession = AuthSession("jwt-token", SessionUser(1, "paul", Role.OWNER))
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private fun sessionWith(saved: AuthSession?) =
        Session(FakeSessionStorage(saved), CoroutineScope(UnconfinedTestDispatcher()))

    @Test
    fun sendsBearerTokenWhenLoggedIn() = runTest {
        var authorization: String? = null
        val engine = MockEngine { request ->
            authorization = request.headers[HttpHeaders.Authorization]
            respond("[]", HttpStatusCode.OK, jsonHeaders)
        }

        createHttpClient(engine, "http://localhost:8080/", sessionWith(paulSession)).get("cars")

        assertEquals("Bearer jwt-token", authorization)
    }

    @Test
    fun sendsNoAuthorizationWhenLoggedOut() = runTest {
        var authorization: String? = "unset"
        val engine = MockEngine { request ->
            authorization = request.headers[HttpHeaders.Authorization]
            respond("[]", HttpStatusCode.OK, jsonHeaders)
        }

        createHttpClient(engine, "http://localhost:8080/", sessionWith(null)).get("cars")

        assertNull(authorization)
    }

    @Test
    fun errorBodyBecomesApiException() = runTest {
        val engine = MockEngine {
            respond("""{"error":"Car 42 not found"}""", HttpStatusCode.NotFound, jsonHeaders)
        }

        try {
            createHttpClient(engine, "http://localhost:8080/", sessionWith(null)).get("cars/42")
            fail("Expected ApiException")
        } catch (e: ApiException) {
            assertEquals(404, e.statusCode)
            assertEquals("Car 42 not found", e.serverMessage)
        }
    }

    @Test
    fun unauthorizedWhileLoggedInSignsOut() = runTest {
        val session = sessionWith(paulSession)
        val engine = MockEngine {
            respond("""{"error":"Invalid token"}""", HttpStatusCode.Unauthorized, jsonHeaders)
        }

        try {
            createHttpClient(engine, "http://localhost:8080/", session).get("users/me")
            fail("Expected ApiException")
        } catch (_: ApiException) {
            assertTrue(session.state.value is SessionState.LoggedOut)
        }
    }
}
