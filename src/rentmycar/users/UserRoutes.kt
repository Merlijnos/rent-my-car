package rentmycar.users

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import rentmycar.auth.UserPrincipal

fun Route.userRoutes(users: UserRepository) = route("/users") {
    post("/register") {
        val user = call.receive<RegisterRequest>().toUser()
        if (users.findByUsername(user.username) != null) {
            throw BadRequestException("Username '${user.username}' is already taken")
        }
        call.respond(HttpStatusCode.Created, users.create(user).toResponse())
    }

    authenticate("auth-jwt") {
        get("/me") {
            val principal = call.principal<UserPrincipal>()!!
            call.respond(principal.user.toResponse())
        }
    }
}
