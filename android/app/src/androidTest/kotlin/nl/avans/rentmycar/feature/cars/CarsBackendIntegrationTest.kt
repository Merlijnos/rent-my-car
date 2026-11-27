package nl.avans.rentmycar.feature.cars

import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import nl.avans.rentmycar.BuildConfig
import nl.avans.rentmycar.core.network.createHttpClient
import nl.avans.rentmycar.core.session.AuthSession
import nl.avans.rentmycar.core.session.Session
import nl.avans.rentmycar.core.session.SessionStorage
import nl.avans.rentmycar.feature.auth.AuthRepository
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Talks to the real backend: start it with `./kotlin run` in the rent-my-car folder first
 * (the emulator reaches it at 10.0.2.2:8080). The database must contain the seeded cars.
 */
class CarsBackendIntegrationTest {
    private val noStorage = object : SessionStorage {
        override suspend fun load(): AuthSession? = null
        override suspend fun save(session: AuthSession) = Unit
        override suspend fun clear() = Unit
    }
    private val session = Session(noStorage, CoroutineScope(SupervisorJob() + Dispatchers.IO))
    private val client = createHttpClient(OkHttp.create(), BuildConfig.BASE_URL, session)
    private val repository = ApiCarRepository(client)

    @Test
    fun getCarsReturnsTheSeededCarsWithALocation() = runBlocking {
        val cars = repository.getCars(category = null, maxPricePerDaypart = null).getOrThrow()

        assertTrue(cars.size >= 4)
        assertTrue(cars.any { it.latitude != null && it.longitude != null })
    }

    @Test
    fun ownerCanLogInCreateACarAndDeleteItAgain() = runBlocking {
        AuthRepository(client, session).login("paul", "paul123").getOrThrow()
        val newCar = CarRequest(
            category = CarCategory.BEV,
            brand = "IntegrationTest",
            model = "Model T",
            pricePerDaypart = 20.0,
            fixedCostsPerYear = 1800.0,
            consumptionPer100Km = 15.0,
        )

        val created = repository.createCar(newCar).getOrThrow()
        repository.deleteCar(created.id).getOrThrow()

        assertTrue(repository.getCar(created.id).isFailure)
    }
}
