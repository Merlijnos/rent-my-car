package rentmycar.reservations

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.util.getValue
import rentmycar.auth.ForbiddenException
import rentmycar.auth.UserPrincipal
import rentmycar.auth.requireRole
import rentmycar.cars.Car
import rentmycar.cars.CarRepository
import rentmycar.users.Role
import rentmycar.users.User

/** The /reservations endpoints. A renter books a car; the car's owner confirms or rejects it. */
fun Route.reservationRoutes(reservations: ReservationRepository, cars: CarRepository) = route("/reservations") {
    authenticate("auth-jwt") {
        get {
            val user = call.principal<UserPrincipal>()!!.user
            val status: ReservationStatus? by call.queryParameters
            val carId: Int? by call.queryParameters
            call.respond(visibleTo(user, reservations, cars, carId, status).map { it.toResponse() })
        }

        get("/{id}") {
            val user = call.principal<UserPrincipal>()!!.user
            val id: Int by call.pathParameters
            val reservation = reservations.existing(id)
            if (!canView(user, reservation, cars)) {
                throw ForbiddenException("Only the renter or the owner of the car can view this reservation")
            }
            call.respond(reservation.toResponse())
        }

        post {
            val renter = call.principal<UserPrincipal>()!!.requireRole(Role.RENTER)
            val request = call.receive<ReservationRequest>()
            val car = cars.findById(request.carId) ?: throw carNotFound(request.carId)
            val reservation = request.toReservation(renter.id, car.pricePerDaypart)
            val conflict = reservations.findActiveOverlap(car.id, reservation.startDate, reservation.endDate)
            if (conflict != null) {
                throw BadRequestException(
                    "Car ${car.id} is already reserved from ${conflict.startDate} to ${conflict.endDate}",
                )
            }
            call.respond(HttpStatusCode.Created, reservations.create(reservation).toResponse())
        }

        post("/{id}/confirm") {
            val owner = call.principal<UserPrincipal>()!!.requireRole(Role.OWNER)
            val id: Int by call.pathParameters
            val reservation = reservations.existing(id)
            requireCarOwner(owner, carOf(cars, reservation), "confirm")
            val confirmed = reservation.confirm()
            if (!reservations.update(confirmed)) throw reservationNotFound(id)
            call.respond(confirmed.toResponse())
        }

        post("/{id}/reject") {
            val owner = call.principal<UserPrincipal>()!!.requireRole(Role.OWNER)
            val id: Int by call.pathParameters
            val reservation = reservations.existing(id)
            requireCarOwner(owner, carOf(cars, reservation), "reject")
            val rejected = reservation.reject()
            if (!reservations.update(rejected)) throw reservationNotFound(id)
            call.respond(rejected.toResponse())
        }

        post("/{id}/cancel") {
            val user = call.principal<UserPrincipal>()!!.user
            val id: Int by call.pathParameters
            val reservation = reservations.existing(id)
            val car = carOf(cars, reservation)
            val allowed = isRenter(user, reservation) || isCarOwner(user, car)
            if (!allowed) {
                throw ForbiddenException("Only the renter or the owner of the car can cancel this reservation")
            }
            val canceled = reservation.cancel()
            if (!reservations.update(canceled)) throw reservationNotFound(id)
            call.respond(canceled.toResponse())
        }
    }
}

private fun visibleTo(
    user: User,
    reservations: ReservationRepository,
    cars: CarRepository,
    carId: Int?,
    status: ReservationStatus?,
): List<Reservation> =
    when (user.role) {
        Role.RENTER -> reservations.findAll(renterId = user.id, carId = carId, status = status)
        Role.OWNER ->
            reservations.findAll(
                carId = carId,
                carIds = cars.findByOwner(user.id).map { it.id },
                status = status,
            )
    }

private fun canView(user: User, reservation: Reservation, cars: CarRepository): Boolean =
    isRenter(user, reservation) || isCarOwner(user, carOf(cars, reservation))

private fun isRenter(user: User, reservation: Reservation): Boolean =
    user.role == Role.RENTER && reservation.renterId == user.id

private fun isCarOwner(user: User, car: Car?): Boolean =
    user.role == Role.OWNER && car != null && car.ownerId == user.id

private fun carOf(cars: CarRepository, reservation: Reservation): Car? = cars.findById(reservation.carId)

private fun requireCarOwner(owner: User, car: Car?, action: String) {
    if (car == null || car.ownerId != owner.id) {
        throw ForbiddenException("Only the owner of the car can $action this reservation")
    }
}

private fun ReservationRepository.existing(id: Int): Reservation = findById(id) ?: throw reservationNotFound(id)

private fun reservationNotFound(id: Int) = NotFoundException("Reservation $id not found")

private fun carNotFound(id: Int) = NotFoundException("Car $id not found")
