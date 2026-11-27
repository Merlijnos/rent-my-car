package nl.avans.rentmycar.core.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
enum class Role { OWNER, RENTER }

data class SessionUser(val id: Int, val username: String, val role: Role)

data class AuthSession(val token: String, val user: SessionUser)

sealed interface SessionState {
    data object Loading : SessionState
    data object LoggedOut : SessionState
    data class LoggedIn(val session: AuthSession) : SessionState
}

/** Holds the JWT and the logged-in user in memory and mirrors them to [SessionStorage]. */
class Session(
    private val storage: SessionStorage,
    scope: CoroutineScope,
) {
    private val _state = MutableStateFlow<SessionState>(SessionState.Loading)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    val token: String?
        get() = (state.value as? SessionState.LoggedIn)?.session?.token

    val user: SessionUser?
        get() = (state.value as? SessionState.LoggedIn)?.session?.user

    init {
        scope.launch {
            val saved = storage.load()
            _state.value = if (saved != null) SessionState.LoggedIn(saved) else SessionState.LoggedOut
        }
    }

    suspend fun signIn(session: AuthSession) {
        storage.save(session)
        _state.value = SessionState.LoggedIn(session)
    }

    suspend fun signOut() {
        storage.clear()
        _state.value = SessionState.LoggedOut
    }
}
