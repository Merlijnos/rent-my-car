package rentmycar.cars

import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.util.getValue

private const val DEFAULT_KM_PER_YEAR = 15_000

/** The /cars endpoints. A missing car throws NotFoundException, which StatusPages turns into a 404. */
fun Route.carRoutes(cars: CarRepository) = route("/cars") {
    get {
        val category: CarCategory? by call.queryParameters
        val maxPricePerDaypart: Double? by call.queryParameters
        call.respond(cars.findAll(category, maxPricePerDaypart).map { it.toResponse() })
    }

    get("/{id}") {
        val id: Int by call.pathParameters
        val car = cars.findById(id) ?: throw carNotFound(id)
        call.respond(car.toResponse())
    }

    post {
        val car = call.receive<CarRequest>().toCar()
        call.respond(HttpStatusCode.Created, cars.create(car).toResponse())
    }

    put("/{id}") {
        val id: Int by call.pathParameters
        val car = call.receive<CarRequest>().toCar(id)
        if (!cars.update(car)) throw carNotFound(id)
        call.respond(car.toResponse())
    }

    delete("/{id}") {
        val id: Int by call.pathParameters
        if (!cars.delete(id)) throw carNotFound(id)
        call.respond(HttpStatusCode.NoContent)
    }

    get("/{id}/costs") {
        val id: Int by call.pathParameters
        val kmPerYear: Int? by call.queryParameters
        val car = cars.findById(id) ?: throw carNotFound(id)
        call.respond(car.toCostsResponse(kmPerYear ?: DEFAULT_KM_PER_YEAR))
    }
}

private fun carNotFound(id: Int) = NotFoundException("Car $id not found")
