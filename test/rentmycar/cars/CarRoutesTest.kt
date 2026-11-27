package rentmycar.cars

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import rentmycar.ErrorResponse
import rentmycar.login
import rentmycar.startApp

class CarRoutesTest {
    private val tesla = CarRequest(CarCategory.BEV, "Tesla", "Model Y", 32.5, 4_200.0, 16.0)

    private suspend fun HttpClient.ownerToken() = login("paul", "paul123")

    private suspend fun HttpClient.postCar(car: CarRequest, token: String) =
        post("/cars") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody(car)
        }

    private suspend fun HttpClient.putCar(id: Int, car: CarRequest, token: String) =
        put("/cars/$id") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody(car)
        }

    @Test
    fun `post creates a car that get returns`() = testApplication {
        val http = startApp()
        val token = http.ownerToken()

        val response = http.postCar(tesla, token)
        assertEquals(HttpStatusCode.Created, response.status)
        val created = response.body<CarResponse>()
        assertEquals("Model Y", created.model)
        assertEquals(1, created.ownerId)

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
        val token = http.ownerToken()
        val id = http.postCar(tesla, token).body<CarResponse>().id

        val response = http.putCar(id, tesla.copy(pricePerDaypart = 29.0), token)
        assertEquals(HttpStatusCode.OK, response.status)

        val fetched = http.get("/cars/$id").body<CarResponse>()
        assertEquals(29.0, fetched.pricePerDaypart)
    }

    @Test
    fun `delete removes the car so get returns 404`() = testApplication {
        val http = startApp()
        val token = http.ownerToken()
        val id = http.postCar(tesla, token).body<CarResponse>().id

        assertEquals(
            HttpStatusCode.NoContent,
            http.delete("/cars/$id") { header("Authorization", "Bearer $token") }.status,
        )
        assertEquals(HttpStatusCode.NotFound, http.get("/cars/$id").status)
    }

    @Test
    fun `unknown id returns 404 with an error message`() = testApplication {
        val http = startApp()
        val token = http.ownerToken()

        val response = http.get("/cars/999")
        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals(ErrorResponse("Car 999 not found"), response.body<ErrorResponse>())
        assertEquals(HttpStatusCode.NotFound, http.putCar(999, tesla, token).status)
        assertEquals(
            HttpStatusCode.NotFound,
            http.delete("/cars/999") { header("Authorization", "Bearer $token") }.status,
        )
    }

    @Test
    fun `post with invalid data returns 400`() = testApplication {
        val http = startApp()
        val token = http.ownerToken()

        val zeroConsumption = http.postCar(tesla.copy(consumptionPer100Km = 0.0), token)
        assertEquals(HttpStatusCode.BadRequest, zeroConsumption.status)
        assertEquals(
            ErrorResponse("consumptionPer100Km must be greater than 0"),
            zeroConsumption.body<ErrorResponse>(),
        )

        val iceWithoutFuel = http.postCar(tesla.copy(category = CarCategory.ICE), token)
        assertEquals(HttpStatusCode.BadRequest, iceWithoutFuel.status)

        val unknownCategory =
            http.post("/cars") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $token")
                setBody(
                    """{"category":"STEAM","brand":"Stanley","model":"Steamer","pricePerDaypart":10.0,"fixedCostsPerYear":1.0,"consumptionPer100Km":1.0}""",
                )
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
        val token = http.ownerToken()
        val id = http.postCar(tesla, token).body<CarResponse>().id

        val default = http.get("/cars/$id/costs").body<CarCostsResponse>()
        assertEquals(15_000, default.kmPerYear)
        assertEquals(0.056, default.costPerKm, 0.0001)
        assertEquals(4_200.0 + 0.056 * 15_000, default.totalCostOfOwnership, 0.0001)

        val custom = http.get("/cars/$id/costs?kmPerYear=10000").body<CarCostsResponse>()
        assertEquals(4_200.0 + 0.056 * 10_000, custom.totalCostOfOwnership, 0.0001)
    }

    @Test
    fun `renter cannot create a car`() = testApplication {
        val http = startApp()
        val renterToken = http.login("lisa", "lisa123")

        val response = http.postCar(tesla, renterToken)
        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertEquals(ErrorResponse("Role OWNER is required"), response.body<ErrorResponse>())
    }

    @Test
    fun `creating a car without a token returns 401`() = testApplication {
        val http = startApp()

        val response =
            http.post("/cars") {
                contentType(ContentType.Application.Json)
                setBody(tesla)
            }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `owner cannot update another owners car`() = testApplication {
        val http = startApp()
        val paulToken = http.login("paul", "paul123")
        val saraToken = http.login("sara", "sara123")
        val carId = http.postCar(tesla, paulToken).body<CarResponse>().id

        val response = http.putCar(carId, tesla.copy(pricePerDaypart = 10.0), saraToken)
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `seeded cars have a location in the Breda and Den Bosch area`() = testApplication {
        val http = startApp()

        val cars = http.get("/cars").body<List<CarResponse>>()
        assertTrue(cars.isNotEmpty())
        cars.forEach {
            assertTrue(it.latitude!! in 51.5..51.8, "latitude of car ${it.id}")
            assertTrue(it.longitude!! in 4.6..5.5, "longitude of car ${it.id}")
        }
    }

    @Test
    fun `post and put store the location and get returns it`() = testApplication {
        val http = startApp()
        val token = http.ownerToken()
        val located = tesla.copy(latitude = 51.589, longitude = 4.776)

        val created = http.postCar(located, token).body<CarResponse>()
        assertEquals(51.589, created.latitude)
        assertEquals(4.776, created.longitude)

        val moved = http.putCar(created.id, located.copy(latitude = 51.699, longitude = 5.304), token)
        assertEquals(HttpStatusCode.OK, moved.status)
        val fetched = http.get("/cars/${created.id}").body<CarResponse>()
        assertEquals(51.699, fetched.latitude)
        assertEquals(5.304, fetched.longitude)
    }

    @Test
    fun `a car without a location returns null coordinates`() = testApplication {
        val http = startApp()
        val token = http.ownerToken()

        val created = http.postCar(tesla, token).body<CarResponse>()
        assertEquals(null, created.latitude)
        assertEquals(null, created.longitude)
    }

    @Test
    fun `invalid location returns 400`() = testApplication {
        val http = startApp()
        val token = http.ownerToken()

        val onlyLatitude = http.postCar(tesla.copy(latitude = 51.589), token)
        assertEquals(HttpStatusCode.BadRequest, onlyLatitude.status)
        assertEquals(
            ErrorResponse("latitude and longitude must both be set or both be empty"),
            onlyLatitude.body<ErrorResponse>(),
        )

        val latitudeTooHigh = http.postCar(tesla.copy(latitude = 91.0, longitude = 4.776), token)
        assertEquals(HttpStatusCode.BadRequest, latitudeTooHigh.status)
        assertEquals(ErrorResponse("latitude must be between -90 and 90"), latitudeTooHigh.body<ErrorResponse>())

        val longitudeTooLow = http.postCar(tesla.copy(latitude = 51.589, longitude = -181.0), token)
        assertEquals(HttpStatusCode.BadRequest, longitudeTooLow.status)
        assertEquals(ErrorResponse("longitude must be between -180 and 180"), longitudeTooLow.body<ErrorResponse>())

        val id = http.postCar(tesla, token).body<CarResponse>().id
        assertEquals(HttpStatusCode.BadRequest, http.putCar(id, tesla.copy(longitude = 4.776), token).status)
    }
}
