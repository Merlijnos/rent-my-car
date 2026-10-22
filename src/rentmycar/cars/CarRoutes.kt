package rentmycar.cars

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
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
import rentmycar.auth.ForbiddenException
import rentmycar.auth.UserPrincipal
import rentmycar.auth.requireRole
import rentmycar.users.Role

private const val DEFAULT_KM_PER_YEAR = 15_000

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

    get("/{id}/costs") {
        val id: Int by call.pathParameters
        val kmPerYear: Int? by call.queryParameters
        val car = cars.findById(id) ?: throw carNotFound(id)
        call.respond(car.toCostsResponse(kmPerYear ?: DEFAULT_KM_PER_YEAR))
    }

    authenticate("auth-jwt") {
        post {
            val owner = call.principal<UserPrincipal>()!!.requireRole(Role.OWNER)
            val car = call.receive<CarRequest>().toCar(ownerId = owner.id)
            call.respond(HttpStatusCode.Created, cars.create(car).toResponse())
        }

        put("/{id}") {
            val owner = call.principal<UserPrincipal>()!!.requireRole(Role.OWNER)
            val id: Int by call.pathParameters
            val existing = cars.findById(id) ?: throw carNotFound(id)
            if (existing.ownerId != owner.id) {
                throw ForbiddenException("Only the owner can update this car")
            }
            val car = call.receive<CarRequest>().toCar(ownerId = owner.id, id = id)
            if (!cars.update(car)) throw carNotFound(id)
            call.respond(car.toResponse())
        }

        delete("/{id}") {
            val owner = call.principal<UserPrincipal>()!!.requireRole(Role.OWNER)
            val id: Int by call.pathParameters
            val existing = cars.findById(id) ?: throw carNotFound(id)
            if (existing.ownerId != owner.id) {
                throw ForbiddenException("Only the owner can delete this car")
            }
            if (!cars.delete(id)) throw carNotFound(id)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private fun carNotFound(id: Int) = NotFoundException("Car $id not found")
