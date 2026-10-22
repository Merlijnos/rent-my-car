package rentmycar.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import rentmycar.ErrorResponse

fun Route.authRoutes(jwt: JwtService) = route("/auth") {
    post("/login") {
        val request = call.receive<LoginRequest>()
        val token = jwt.login(request.username, request.password)
        if (token == null) {
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid username or password"))
            return@post
        }
        call.respond(TokenResponse(token))
    }
}
