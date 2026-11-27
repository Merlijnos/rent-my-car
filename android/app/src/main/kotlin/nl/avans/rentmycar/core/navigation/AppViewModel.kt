package nl.avans.rentmycar.core.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import nl.avans.rentmycar.core.session.Session
import nl.avans.rentmycar.core.session.SessionState

class AppViewModel(private val session: Session) : ViewModel() {
    val sessionState: StateFlow<SessionState> = session.state

    fun logout() {
        viewModelScope.launch { session.signOut() }
    }
}
