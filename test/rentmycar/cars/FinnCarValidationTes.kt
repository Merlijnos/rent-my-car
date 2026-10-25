
package rentmycar.cars

import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import rentmycar.ErrorResponse
import rentmycar.login
import rentmycar.startApp

class FinnCarValidationTest {

    private val testCar = CarRequest(
        category = CarCategory.BEV,
        brand = "BMW",
        model = "i4",
        pricePerDaypart = 30.0,
        fixedCostsPerYear = 3500.0,
        consumptionPer100Km = 17.0
    )

    @Test
    fun `empty brand returns 400`() = testApplication {
        val http = startApp()
        val token = http.login("paul", "paul123")

        val response = http.post("/cars") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody(testCar.copy(brand = "   "))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals(
            ErrorResponse("brand must be 1 to 50 characters"),
            response.body<ErrorResponse>()
        )
    }

    @Test
    fun `empty model returns 400`() = testApplication {
        val http = startApp()
        val token = http.login("paul", "paul123")

        val response = http.post("/cars") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody(testCar.copy(model = ""))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `other owner cannot delete my car`() = testApplication {
        val http = startApp()
        val paulToken = http.login("paul", "paul123")
        val saraToken = http.login("sara", "sara123")

        val created = http.post("/cars") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $paulToken")
            setBody(testCar)
        }.body<CarResponse>()

        val deleteResponse = http.delete("/cars/${created.id}") {
            header("Authorization", "Bearer $saraToken")
        }

        assertEquals(HttpStatusCode.Forbidden, deleteResponse.status)

        val getResponse = http.get("/cars/${created.id}")
        assertEquals(HttpStatusCode.OK, getResponse.status)
    }
}
