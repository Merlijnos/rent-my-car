package rentmycar.reservations

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import rentmycar.ErrorResponse
import rentmycar.cars.CarCategory
import rentmycar.cars.CarRequest
import rentmycar.cars.CarResponse
import rentmycar.login
import rentmycar.startApp
import rentmycar.users.UserResponse

class ReservationRoutesTest {
    private suspend fun HttpClient.book(carId: Int, start: String, end: String, token: String): HttpResponse =
        post("/reservations") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody(ReservationRequest(carId, start, end))
        }

    private suspend fun HttpClient.car(modelPrefix: String): CarResponse =
        get("/cars").body<List<CarResponse>>().first { it.model.startsWith(modelPrefix) }

    private suspend fun HttpClient.me(token: String): UserResponse =
        get("/users/me") { header("Authorization", "Bearer $token") }.body()

    @Test
    fun `post creates a reservation that get returns`() = testApplication {
        val http = startApp()
        val lisa = http.login("lisa", "lisa123")
        val polo = http.car("Polo")

        val response = http.book(polo.id, "2026-09-01", "2026-09-03", lisa)
        assertEquals(HttpStatusCode.Created, response.status)
        val created = response.body<ReservationResponse>()
        assertEquals(polo.id, created.carId)
        assertEquals(http.me(lisa).id, created.renterId)
        assertEquals("2026-09-01", created.startDate)
        assertEquals("2026-09-03", created.endDate)
        assertEquals(6, created.dayparts)
        assertEquals(90.0, created.totalPrice)
        assertEquals(ReservationStatus.PENDING, created.status)

        val fetched =
            http.get("/reservations/${created.id}") { header("Authorization", "Bearer $lisa") }
        assertEquals(HttpStatusCode.OK, fetched.status)
        assertEquals(created, fetched.body<ReservationResponse>())
    }

    @Test
    fun `overlapping an active reservation returns 400 and the next day is free`() = testApplication {
        val http = startApp()
        val lisa = http.login("lisa", "lisa123")
        val polo = http.car("Polo")

        val overlap = http.book(polo.id, "2026-07-08", "2026-07-09", lisa)
        assertEquals(HttpStatusCode.BadRequest, overlap.status)
        assertEquals(
            ErrorResponse("Car ${polo.id} is already reserved from 2026-07-06 to 2026-07-08"),
            overlap.body<ErrorResponse>(),
        )

        val nextDay = http.book(polo.id, "2026-07-09", "2026-07-09", lisa)
        assertEquals(HttpStatusCode.Created, nextDay.status)
        assertEquals(2, nextDay.body<ReservationResponse>().dayparts)
    }

    @Test
    fun `invalid dates or an unknown car return 400 and 404`() = testApplication {
        val http = startApp()
        val lisa = http.login("lisa", "lisa123")
        val polo = http.car("Polo")

        val reversed = http.book(polo.id, "2026-09-03", "2026-09-01", lisa)
        assertEquals(HttpStatusCode.BadRequest, reversed.status)
        assertEquals(ErrorResponse("endDate must not be before startDate"), reversed.body<ErrorResponse>())

        val badDate = http.book(polo.id, "tomorrow", "2026-09-01", lisa)
        assertEquals(HttpStatusCode.BadRequest, badDate.status)
        assertEquals(
            ErrorResponse("startDate must be a date in the form yyyy-MM-dd"),
            badDate.body<ErrorResponse>(),
        )

        val missing = http.book(999, "2026-09-01", "2026-09-01", lisa)
        assertEquals(HttpStatusCode.NotFound, missing.status)
        assertEquals(ErrorResponse("Car 999 not found"), missing.body<ErrorResponse>())
    }

    @Test
    fun `renter sees own reservations and owner sees reservations of own cars`() = testApplication {
        val http = startApp()
        val lisa = http.login("lisa", "lisa123")
        val paul = http.login("paul", "paul123")
        val sara = http.login("sara", "sara123")
        val lisaId = http.me(lisa).id

        val lisas =
            http.get("/reservations") { header("Authorization", "Bearer $lisa") }.body<List<ReservationResponse>>()
        assertTrue(lisas.isNotEmpty())
        assertTrue(lisas.all { it.renterId == lisaId })
        assertTrue(lisas.any { it.status == ReservationStatus.CONFIRMED && it.startDate == "2026-07-06" })

        val pauls =
            http.get("/reservations") { header("Authorization", "Bearer $paul") }.body<List<ReservationResponse>>()
        assertTrue(pauls.any { it.startDate == "2026-07-06" && it.status == ReservationStatus.CONFIRMED })
        assertTrue(pauls.none { it.startDate == "2026-08-10" })

        val pending =
            http.get("/reservations?status=PENDING") { header("Authorization", "Bearer $sara") }
                .body<List<ReservationResponse>>()
        assertEquals("2026-08-10", pending.single().startDate)
        assertEquals(ReservationStatus.PENDING, pending.single().status)
    }

    @Test
    fun `owner confirms and the renter can cancel so the dates open up`() = testApplication {
        val http = startApp()
        val lisa = http.login("lisa", "lisa123")
        val tom = http.login("tom", "tom123")
        val paul = http.login("paul", "paul123")
        val sara = http.login("sara", "sara123")
        val model3 = http.car("Model 3")
        val created = http.book(model3.id, "2026-11-01", "2026-11-01", lisa).body<ReservationResponse>()

        val renterConfirm =
            http.post("/reservations/${created.id}/confirm") { header("Authorization", "Bearer $lisa") }
        assertEquals(HttpStatusCode.Forbidden, renterConfirm.status)
        assertEquals(ErrorResponse("Role OWNER is required"), renterConfirm.body<ErrorResponse>())

        val otherOwner =
            http.post("/reservations/${created.id}/confirm") { header("Authorization", "Bearer $sara") }
        assertEquals(HttpStatusCode.Forbidden, otherOwner.status)
        assertEquals(
            ErrorResponse("Only the owner of the car can confirm this reservation"),
            otherOwner.body<ErrorResponse>(),
        )

        val confirmed =
            http.post("/reservations/${created.id}/confirm") { header("Authorization", "Bearer $paul") }
        assertEquals(HttpStatusCode.OK, confirmed.status)
        assertEquals(ReservationStatus.CONFIRMED, confirmed.body<ReservationResponse>().status)

        val again =
            http.post("/reservations/${created.id}/confirm") { header("Authorization", "Bearer $paul") }
        assertEquals(HttpStatusCode.BadRequest, again.status)
        assertEquals(ErrorResponse("Only a pending reservation can be confirmed"), again.body<ErrorResponse>())

        val blocked = http.book(model3.id, "2026-11-01", "2026-11-02", tom)
        assertEquals(HttpStatusCode.BadRequest, blocked.status)

        val canceled =
            http.post("/reservations/${created.id}/cancel") { header("Authorization", "Bearer $lisa") }
        assertEquals(HttpStatusCode.OK, canceled.status)
        assertEquals(ReservationStatus.CANCELED, canceled.body<ReservationResponse>().status)

        assertEquals(HttpStatusCode.Created, http.book(model3.id, "2026-11-01", "2026-11-02", tom).status)
    }

    @Test
    fun `owner rejects a pending reservation`() = testApplication {
        val http = startApp()
        val sara = http.login("sara", "sara123")
        val tom = http.login("tom", "tom123")
        val niro = http.car("Niro")
        val pending =
            http.get("/reservations?status=PENDING") { header("Authorization", "Bearer $tom") }
                .body<List<ReservationResponse>>()
                .single()

        val rejected =
            http.post("/reservations/${pending.id}/reject") { header("Authorization", "Bearer $sara") }
        assertEquals(HttpStatusCode.OK, rejected.status)
        assertEquals(ReservationStatus.REJECTED, rejected.body<ReservationResponse>().status)

        assertEquals(HttpStatusCode.Created, http.book(niro.id, "2026-08-10", "2026-08-10", tom).status)
    }

    @Test
    fun `someone else cannot view or cancel a reservation`() = testApplication {
        val http = startApp()
        val tom = http.login("tom", "tom123")
        val sara = http.login("sara", "sara123")
        val lisa = http.login("lisa", "lisa123")
        val poloReservation =
            http.get("/reservations") { header("Authorization", "Bearer $lisa") }
                .body<List<ReservationResponse>>()
                .single { it.startDate == "2026-07-06" }

        val viewed =
            http.get("/reservations/${poloReservation.id}") { header("Authorization", "Bearer $tom") }
        assertEquals(HttpStatusCode.Forbidden, viewed.status)

        val canceled =
            http.post("/reservations/${poloReservation.id}/cancel") { header("Authorization", "Bearer $sara") }
        assertEquals(HttpStatusCode.Forbidden, canceled.status)
        assertEquals(
            ErrorResponse("Only the renter or the owner of the car can cancel this reservation"),
            canceled.body<ErrorResponse>(),
        )
    }

    @Test
    fun `owner cannot delete a car that still has an active reservation`() = testApplication {
        val http = startApp()
        val paul = http.login("paul", "paul123")
        val lisa = http.login("lisa", "lisa123")
        val car =
            http.post("/cars") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $paul")
                setBody(CarRequest(CarCategory.BEV, "Tesla", "Model Y", 32.5, 4_200.0, 16.0))
            }.body<CarResponse>()
        val reservation = http.book(car.id, "2026-10-01", "2026-10-02", lisa).body<ReservationResponse>()

        val denied = http.delete("/cars/${car.id}") { header("Authorization", "Bearer $paul") }
        assertEquals(HttpStatusCode.BadRequest, denied.status)
        assertEquals(ErrorResponse("Car ${car.id} still has active reservations"), denied.body<ErrorResponse>())

        http.post("/reservations/${reservation.id}/cancel") { header("Authorization", "Bearer $lisa") }
        assertEquals(
            HttpStatusCode.NoContent,
            http.delete("/cars/${car.id}") { header("Authorization", "Bearer $paul") }.status,
        )
    }

    @Test
    fun `owner cannot create a reservation and missing auth returns 401`() = testApplication {
        val http = startApp()
        val paul = http.login("paul", "paul123")
        val polo = http.car("Polo")

        val forbidden = http.book(polo.id, "2026-12-01", "2026-12-01", paul)
        assertEquals(HttpStatusCode.Forbidden, forbidden.status)
        assertEquals(ErrorResponse("Role RENTER is required"), forbidden.body<ErrorResponse>())

        val missing =
            http.post("/reservations") {
                contentType(ContentType.Application.Json)
                setBody(ReservationRequest(polo.id, "2026-12-01", "2026-12-01"))
            }
        assertEquals(HttpStatusCode.Unauthorized, missing.status)
        assertEquals(HttpStatusCode.Unauthorized, http.get("/reservations").status)
    }

    @Test
    fun `unknown reservation or a bad id or status returns 404 or 400`() = testApplication {
        val http = startApp()
        val lisa = http.login("lisa", "lisa123")

        val missing = http.get("/reservations/999") { header("Authorization", "Bearer $lisa") }
        assertEquals(HttpStatusCode.NotFound, missing.status)
        assertEquals(ErrorResponse("Reservation 999 not found"), missing.body<ErrorResponse>())
        assertEquals(
            HttpStatusCode.BadRequest,
            http.get("/reservations/abc") { header("Authorization", "Bearer $lisa") }.status,
        )
        assertEquals(
            HttpStatusCode.BadRequest,
            http.get("/reservations?status=MAYBE") { header("Authorization", "Bearer $lisa") }.status,
        )
    }
}