package nl.avans.rentmycar.core

import nl.avans.rentmycar.core.session.AuthSession
import nl.avans.rentmycar.core.session.SessionStorage

class FakeSessionStorage(private var saved: AuthSession? = null) : SessionStorage {
    override suspend fun load(): AuthSession? = saved

    override suspend fun save(session: AuthSession) {
        saved = session
    }

    override suspend fun clear() {
        saved = null
    }
}
