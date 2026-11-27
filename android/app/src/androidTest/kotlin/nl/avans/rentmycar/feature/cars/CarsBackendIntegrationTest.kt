package nl.avans.rentmycar.feature.cars

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import nl.avans.rentmycar.BuildConfig
import nl.avans.rentmycar.core.network.ApiException
import nl.avans.rentmycar.core.network.createHttpClient
import nl.avans.rentmycar.core.session.AuthSession
import nl.avans.rentmycar.core.session.Session
import nl.avans.rentmycar.core.session.SessionStorage
import nl.avans.rentmycar.feature.auth.AuthRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.serialization.Serializable

/**
 * Talks to the real backend, so it needs `./kotlin run` in the rent-my-car folder
 * to be running (emulator address 10.0.2.2:8080) with the seeded database.
 */
class CarsBackendIntegrationTest {
    private val noStorage = object : SessionStorage {
        override suspend fun load(): AuthSession? = null
        override suspend fun save(session: AuthSession) = Unit
        override suspend fun clear() = Unit
    }

    private val anonymousRepository = ApiCarRepository(
        createHttpClient(OkHttp.create(), BuildConfig.BASE_URL, Session(noStorage, CoroutineScope(SupervisorJob() + Dispatchers.IO))),
    )

    private val newCar = CarRequest(
        category = CarCategory.BEV,
        brand = "IntegrationTest",
        model = "Model T",
        pricePerDaypart = 20.0,
        fixedCostsPerYear = 1800.0,
        consumptionPer100Km = 15.0,
        latitude = 51.5719,
        longitude = 4.7683,
    )

    /** Logs in through the real login flow, so the client sends the token like the app does. */
    private suspend fun loggedInClient(username: String, password: String): HttpClient {
        val session = Session(noStorage, CoroutineScope(SupervisorJob() + Dispatchers.IO))
        val client = createHttpClient(OkHttp.create(), BuildConfig.BASE_URL, session)
        AuthRepository(client, session).login(username, password).getOrThrow()
        return client
    }

    @Test
    fun getCarsReturnsTheSeededCars() = runBlocking {
        val cars = anonymousRepository.getCars(category = null, maxPricePerDaypart = null).getOrThrow()

        assertTrue("Expected the seeded cars, got ${cars.size}", cars.size >= 4)
        assertTrue(cars.any { it.brand == "Tesla" })
    }

    @Test
    fun categoryFilterOnlyReturnsThatCategory() = runBlocking {
        val cars = anonymousRepository.getCars(category = CarCategory.BEV, maxPricePerDaypart = null).getOrThrow()

        assertTrue(cars.isNotEmpty())
        assertTrue(cars.all { it.category == CarCategory.BEV })
    }

    @Test
    fun unknownCarGivesAnError() = runBlocking {
        assertTrue(anonymousRepository.getCar(999_999).isFailure)
    }

    @Test
    fun ownerCanCreateUpdateReadAndDeleteACar() = runBlocking {
        val repository = ApiCarRepository(loggedInClient("paul", "paul123"))

        val created = repository.createCar(newCar).getOrThrow()
        assertEquals(51.5719, created.latitude!!, 0.00001)
        assertEquals(4.7683, created.longitude!!, 0.00001)

        val updated = repository.updateCar(created.id, newCar.copy(model = "Model T2", latitude = 51.6, longitude = 4.8)).getOrThrow()
        assertEquals("Model T2", updated.model)

        val readBack = repository.getCar(created.id).getOrThrow()
        assertEquals("Model T2", readBack.model)
        assertEquals(51.6, readBack.latitude!!, 0.00001)

        repository.deleteCar(created.id).getOrThrow()
        assertTrue(repository.getCar(created.id).isFailure)
    }

    @Test
    fun invalidLocationIsRejectedWithAMessage() = runBlocking {
        val repository = ApiCarRepository(loggedInClient("paul", "paul123"))

        val error = repository.createCar(newCar.copy(latitude = 95.0)).exceptionOrNull()

        assertTrue(error is ApiException)
        assertEquals(400, (error as ApiException).statusCode)
        assertNotNull(error.serverMessage)
    }

    @Test
    fun deletingACarWithAnActiveReservationShowsTheBackendMessage() = runBlocking {
        val ownerRepository = ApiCarRepository(loggedInClient("paul", "paul123"))
        val car = ownerRepository.createCar(newCar.copy(model = "Reserved")).getOrThrow()
        loggedInClient("lisa", "lisa123").post("reservations") {
            contentType(ContentType.Application.Json)
            setBody(ReservationBody(car.id, "2027-03-01", "2027-03-02"))
        }

        val error = ownerRepository.deleteCar(car.id).exceptionOrNull()

        assertTrue(error is ApiException)
        assertEquals(400, (error as ApiException).statusCode)
        assertEquals("Car ${car.id} still has active reservations", error.serverMessage)
    }
}

@Serializable
private data class ReservationBody(val carId: Int, val startDate: String, val endDate: String)
