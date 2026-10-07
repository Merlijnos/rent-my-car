package rentmycar.cars

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import rentmycar.ErrorResponse
import rentmycar.module

class CarRoutesTest {
    private val tesla = CarRequest(CarCategory.BEV, "Tesla", "Model Y", 32.5, 4_200.0, 16.0)

    private fun ApplicationTestBuilder.startApp(): HttpClient {
        application { module("jdbc:h2:mem:${UUID.randomUUID()};DB_CLOSE_DELAY=-1") }
        return createClient { install(ContentNegotiation) { json() } }
    }

    private suspend fun HttpClient.postCar(car: CarRequest) =
        post("/cars") {
            contentType(ContentType.Application.Json)
            setBody(car)
        }

    private suspend fun HttpClient.putCar(id: Int, car: CarRequest) =
        put("/cars/$id") {
            contentType(ContentType.Application.Json)
            setBody(car)
        }

    @Test
    fun `post creates a car that get returns`() = testApplication {
        val http = startApp()

        val response = http.postCar(tesla)
        assertEquals(HttpStatusCode.Created, response.status)
        val created = response.body<CarResponse>()
        assertEquals("Model Y", created.model)

        val fetched = http.get("/cars/${created.id}")
        assertEquals(HttpStatusCode.OK, fetched.status)
        assertEquals(created, fetched.body<CarResponse>())
    }

    @Test
    fun `get cars filters on category and max price`() = testApplication {
        val http = startApp()
        val all = http.get("/cars").body<List<CarResponse>>()

        val bevs = http.get("/cars?category=BEV").body<List<CarResponse>>()
        assertTrue(bevs.isNotEmpty())
        assertEquals(all.filter { it.category == CarCategory.BEV }, bevs)

        val cheapIce = http.get("/cars?category=ICE&maxPricePerDaypart=20").body<List<CarResponse>>()
        assertTrue(cheapIce.isNotEmpty())
        assertEquals(all.filter { it.category == CarCategory.ICE && it.pricePerDaypart <= 20.0 }, cheapIce)
    }

    @Test
    fun `put replaces the car`() = testApplication {
        val http = startApp()
        val id = http.postCar(tesla).body<CarResponse>().id

        val response = http.putCar(id, tesla.copy(pricePerDaypart = 29.0))
        assertEquals(HttpStatusCode.OK, response.status)

        val fetched = http.get("/cars/$id").body<CarResponse>()
        assertEquals(29.0, fetched.pricePerDaypart)
    }

    @Test
    fun `delete removes the car so get returns 404`() = testApplication {
        val http = startApp()
        val id = http.postCar(tesla).body<CarResponse>().id

        assertEquals(HttpStatusCode.NoContent, http.delete("/cars/$id").status)
        assertEquals(HttpStatusCode.NotFound, http.get("/cars/$id").status)
    }

    @Test
    fun `unknown id returns 404 with an error message`() = testApplication {
        val http = startApp()

        val response = http.get("/cars/999")
        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals(ErrorResponse("Car 999 not found"), response.body<ErrorResponse>())
        assertEquals(HttpStatusCode.NotFound, http.putCar(999, tesla).status)
        assertEquals(HttpStatusCode.NotFound, http.delete("/cars/999").status)
    }

    @Test
    fun `post with invalid data returns 400`() = testApplication {
        val http = startApp()

        val zeroConsumption = http.postCar(tesla.copy(consumptionPer100Km = 0.0))
        assertEquals(HttpStatusCode.BadRequest, zeroConsumption.status)
        assertEquals(
            ErrorResponse("consumptionPer100Km must be greater than 0"),
            zeroConsumption.body<ErrorResponse>(),
        )

        val iceWithoutFuel = http.postCar(tesla.copy(category = CarCategory.ICE))
        assertEquals(HttpStatusCode.BadRequest, iceWithoutFuel.status)

        val unknownCategory =
            http.post("/cars") {
                contentType(ContentType.Application.Json)
                setBody("""{"category":"STEAM","brand":"Stanley","model":"Steamer","pricePerDaypart":10.0,"fixedCostsPerYear":1.0,"consumptionPer100Km":1.0}""")
            }
        assertEquals(HttpStatusCode.BadRequest, unknownCategory.status)
    }

    @Test
    fun `invalid id or category in the url returns 400`() = testApplication {
        val http = startApp()

        assertEquals(HttpStatusCode.BadRequest, http.get("/cars/abc").status)
        assertEquals(HttpStatusCode.BadRequest, http.get("/cars?category=STEAM").status)
        assertEquals(HttpStatusCode.BadRequest, http.get("/cars/1/costs?kmPerYear=-5").status)
    }

    @Test
    fun `costs are calculated for 15000 km per year by default`() = testApplication {
        val http = startApp()
        val id = http.postCar(tesla).body<CarResponse>().id

        val default = http.get("/cars/$id/costs").body<CarCostsResponse>()
        assertEquals(15_000, default.kmPerYear)
        assertEquals(0.056, default.costPerKm, 0.0001)
        assertEquals(4_200.0 + 0.056 * 15_000, default.totalCostOfOwnership, 0.0001)

        val custom = http.get("/cars/$id/costs?kmPerYear=10000").body<CarCostsResponse>()
        assertEquals(4_200.0 + 0.056 * 10_000, custom.totalCostOfOwnership, 0.0001)
    }
}
