package nl.avans.rentmycar.feature.cars

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import nl.avans.rentmycar.core.FakeSessionStorage
import nl.avans.rentmycar.core.network.ApiException
import nl.avans.rentmycar.core.network.appJson
import nl.avans.rentmycar.core.network.createHttpClient
import nl.avans.rentmycar.core.session.Session
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CarJsonMappingTest {
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private val poloJson = """
        {"id":1,"category":"ICE","brand":"Volkswagen","model":"Polo 1.0 TSI","pricePerDaypart":15.0,
         "fixedCostsPerYear":2400.0,"consumptionPer100Km":5.4,"fuelType":"PETROL","ownerId":1,
         "latitude":51.5719,"longitude":4.7683}
    """.trimIndent()

    private val teslaJson = """
        {"id":5,"category":"BEV","brand":"Tesla","model":"Model 3","pricePerDaypart":45.0,
         "fixedCostsPerYear":4000.0,"consumptionPer100Km":14.5,"fuelType":null,"ownerId":1,
         "latitude":null,"longitude":null}
    """.trimIndent()

    private fun repository(engine: MockEngine) =
        ApiCarRepository(
            createHttpClient(engine, "http://localhost:8080/", Session(FakeSessionStorage(), CoroutineScope(UnconfinedTestDispatcher()))),
        )

    @Test
    fun carWithLocationIsMapped() {
        val car = appJson.decodeFromString<Car>(poloJson)

        assertEquals(CarCategory.ICE, car.category)
        assertEquals(FuelType.PETROL, car.fuelType)
        assertEquals(51.5719, car.location!!.latitude, 0.0)
        assertEquals(4.7683, car.location!!.longitude, 0.0)
    }

    @Test
    fun carWithoutFuelTypeAndLocationIsMapped() {
        val car = appJson.decodeFromString<Car>(teslaJson)

        assertEquals(CarCategory.BEV, car.category)
        assertNull(car.fuelType)
        assertNull(car.location)
    }

    @Test
    fun unknownFieldsFromTheBackendAreIgnored() {
        val car = appJson.decodeFromString<Car>(teslaJson.replace("\"id\":5", "\"id\":5,\"extra\":true"))

        assertEquals(5, car.id)
    }

    @Test
    fun getCarsSendsFilterParametersAndMapsTheList() = runTest {
        var query = ""
        val engine = MockEngine { request ->
            query = request.url.encodedQuery
            respond("[$poloJson,$teslaJson]", HttpStatusCode.OK, jsonHeaders)
        }

        val cars = repository(engine).getCars(CarCategory.BEV, 25.0).getOrThrow()

        assertEquals(listOf(1, 5), cars.map { it.id })
        assertTrue("category=BEV" in query)
        assertTrue("maxPricePerDaypart=25.0" in query)
    }

    @Test
    fun getCarsWithoutFiltersSendsNoParameters() = runTest {
        var query = "unset"
        val engine = MockEngine { request ->
            query = request.url.encodedQuery
            respond("[]", HttpStatusCode.OK, jsonHeaders)
        }

        repository(engine).getCars(null, null).getOrThrow()

        assertEquals("", query)
    }

    @Test
    fun costsResponseIsMapped() = runTest {
        val engine = MockEngine { request ->
            assertEquals("/cars/1/costs", request.url.encodedPath)
            assertEquals("kmPerYear=20000", request.url.encodedQuery)
            respond(
                """{"carId":1,"kmPerYear":20000,"costPerKm":0.1053,"totalCostOfOwnership":4506.0}""",
                HttpStatusCode.OK,
                jsonHeaders,
            )
        }

        val costs = repository(engine).getCosts(1, 20_000).getOrThrow()

        assertEquals(CarCosts(1, 20_000, 0.1053, 4506.0), costs)
    }

    @Test
    fun createCarPostsTheRequestAsJson() = runTest {
        var method: HttpMethod? = null
        var body = ""
        val engine = MockEngine { request ->
            method = request.method
            body = String(request.body.toByteArray())
            respond(poloJson, HttpStatusCode.Created, jsonHeaders)
        }
        val request = CarRequest(CarCategory.ICE, "Volkswagen", "Polo 1.0 TSI", 15.0, 2400.0, 5.4, FuelType.PETROL, 51.5719, 4.7683)

        val created = repository(engine).createCar(request).getOrThrow()

        assertEquals(HttpMethod.Post, method)
        val sent = Json.parseToJsonElement(body).jsonObject
        assertEquals("ICE", sent.getValue("category").jsonPrimitive.content)
        assertEquals("PETROL", sent.getValue("fuelType").jsonPrimitive.content)
        assertEquals("51.5719", sent.getValue("latitude").jsonPrimitive.content)
        assertEquals(1, created.id)
    }

    @Test
    fun deleteAccepts204WithoutBody() = runTest {
        val engine = MockEngine { respond("", HttpStatusCode.NoContent) }

        assertTrue(repository(engine).deleteCar(1).isSuccess)
    }

    @Test
    fun deleteRefusedWith400KeepsTheServerMessage() = runTest {
        val engine = MockEngine {
            respond("""{"error":"Car 1 still has active reservations"}""", HttpStatusCode.BadRequest, jsonHeaders)
        }

        val failure = repository(engine).deleteCar(1).exceptionOrNull() as ApiException

        assertEquals(400, failure.statusCode)
        assertEquals("Car 1 still has active reservations", failure.serverMessage)
    }
}
