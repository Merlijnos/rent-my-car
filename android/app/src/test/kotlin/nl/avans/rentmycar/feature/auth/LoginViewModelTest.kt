package nl.avans.rentmycar.feature.auth

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import nl.avans.rentmycar.R
import nl.avans.rentmycar.core.FakeSessionStorage
import nl.avans.rentmycar.core.common.UserMessage
import nl.avans.rentmycar.core.network.createHttpClient
import nl.avans.rentmycar.core.session.Role
import nl.avans.rentmycar.core.session.Session
import nl.avans.rentmycar.core.session.SessionState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(engine: MockEngine): Pair<LoginViewModel, Session> {
        val session = Session(FakeSessionStorage(), CoroutineScope(dispatcher))
        val client = createHttpClient(engine, "http://localhost:8080/", session)
        return LoginViewModel(AuthRepository(client, session)) to session
    }

    @Test
    fun cannotSubmitWithEmptyFields() {
        val (viewModel, _) = viewModel(MockEngine { respond("", HttpStatusCode.OK) })

        assertFalse(viewModel.uiState.value.canSubmit)
        viewModel.onUsernameChange("paul")
        assertFalse(viewModel.uiState.value.canSubmit)
        viewModel.onPasswordChange("paul123")
        assertTrue(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun successfulLoginStoresTokenAndUserInSession() = runTest {
        val engine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/auth/login" -> respond("""{"token":"jwt-token"}""", HttpStatusCode.OK, jsonHeaders)
                "/users/me" -> {
                    assertEquals("Bearer jwt-token", request.headers[HttpHeaders.Authorization])
                    respond(
                        """{"id":1,"username":"paul","email":"paul@rentmycar.nl","role":"OWNER"}""",
                        HttpStatusCode.OK,
                        jsonHeaders,
                    )
                }
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val (viewModel, session) = viewModel(engine)

        viewModel.onUsernameChange("paul")
        viewModel.onPasswordChange("paul123")
        viewModel.login()
        viewModel.uiState.first { !it.isLoading }

        val state = session.state.value as SessionState.LoggedIn
        assertEquals("jwt-token", state.session.token)
        assertEquals(Role.OWNER, state.session.user.role)
        assertEquals(1, state.session.user.id)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun wrongPasswordShowsServerMessage() = runTest {
        val engine = MockEngine {
            respond("""{"error":"Invalid credentials"}""", HttpStatusCode.Unauthorized, jsonHeaders)
        }
        val (viewModel, session) = viewModel(engine)

        viewModel.onUsernameChange("paul")
        viewModel.onPasswordChange("wrong")
        viewModel.login()
        viewModel.uiState.first { !it.isLoading }

        assertEquals(
            UserMessage(R.string.error_server_message, "Invalid credentials"),
            viewModel.uiState.value.error,
        )
        assertTrue(session.state.value is SessionState.LoggedOut)
        assertFalse(viewModel.uiState.value.isLoading)
    }
}
