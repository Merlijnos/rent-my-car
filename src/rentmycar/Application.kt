package rentmycar

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import rentmycar.cars.CarRepository
import rentmycar.cars.ExposedCarRepository
import rentmycar.cars.carRoutes

@Serializable
data class ErrorResponse(val error: String)

/** Installs JSON and error handling, connects the database and adds the routes. */
fun Application.module(databaseUrl: String) {
    install(ContentNegotiation) { json() }
    install(StatusPages) {
        exception<NotFoundException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, ErrorResponse(cause.message ?: "Not found"))
        }
        exception<BadRequestException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(cause.message ?: "Bad request"))
        }
        exception<IllegalArgumentException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(cause.message ?: "Invalid input"))
        }
    }

    val database = connectDatabase(databaseUrl)
    val cars: CarRepository = ExposedCarRepository(database)

    routing {
        carRoutes(cars)
    }
}
