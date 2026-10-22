package rentmycar

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import rentmycar.auth.ForbiddenException
import rentmycar.auth.JwtConfig
import rentmycar.auth.JwtService
import rentmycar.auth.UserPrincipal
import rentmycar.auth.authRoutes
import rentmycar.cars.CarRepository
import rentmycar.cars.carRoutes
import rentmycar.users.UserRepository
import rentmycar.users.userRoutes

@Serializable
data class ErrorResponse(val error: String)

fun Application.module(databaseUrl: String, jwtConfig: JwtConfig) {
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
        exception<ForbiddenException> { call, cause ->
            call.respond(HttpStatusCode.Forbidden, ErrorResponse(cause.message ?: "Forbidden"))
        }
    }

    val database = connectDatabase(databaseUrl)
    val users = UserRepository(database)
    val cars = CarRepository(database)
    val jwt = JwtService(jwtConfig, users)

    install(Authentication) {
        jwt("auth-jwt") {
            realm = jwt.realm
            verifier(jwt.verifier)
            validate { credential ->
                if (!credential.payload.audience.contains(jwt.audience)) return@validate null
                val userId = credential.payload.getClaim("userId").asInt()
                jwt.findUserFromClaims(userId)?.let { UserPrincipal(it) }
            }
        }
    }

    routing {
        authRoutes(jwt)
        userRoutes(users)
        carRoutes(cars)
    }
}
