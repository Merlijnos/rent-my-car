package nl.avans.rentmycar.core.session

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

interface SessionStorage {
    suspend fun load(): AuthSession?
    suspend fun save(session: AuthSession)
    suspend fun clear()
}

private val Context.sessionDataStore by preferencesDataStore(name = "session")

private val tokenKey = stringPreferencesKey("token")
private val userIdKey = intPreferencesKey("user_id")
private val usernameKey = stringPreferencesKey("username")
private val roleKey = stringPreferencesKey("role")

class DataStoreSessionStorage(context: Context) : SessionStorage {
    private val dataStore = context.applicationContext.sessionDataStore

    override suspend fun load(): AuthSession? = dataStore.data.first().toAuthSession()

    override suspend fun save(session: AuthSession) {
        dataStore.edit {
            it[tokenKey] = session.token
            it[userIdKey] = session.user.id
            it[usernameKey] = session.user.username
            it[roleKey] = session.user.role.name
        }
    }

    override suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private fun Preferences.toAuthSession(): AuthSession? {
        val token = this[tokenKey] ?: return null
        val id = this[userIdKey] ?: return null
        val username = this[usernameKey] ?: return null
        val role = Role.entries.firstOrNull { it.name == this[roleKey] } ?: return null
        return AuthSession(token, SessionUser(id, username, role))
    }
}
