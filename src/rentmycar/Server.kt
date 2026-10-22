package rentmycar

import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.getAs
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.serialization.Serializable
import rentmycar.auth.JwtConfig

@Serializable
data class Config(
    val host: String,
    val port: Int,
    val databaseUrl: String,
    val jwt: JwtConfig,
)

fun main() {
    val config = ApplicationConfig("application.yaml").getAs<Config>()

    embeddedServer(Netty, host = config.host, port = config.port) {
        module(config.databaseUrl, config.jwt)
    }.start(wait = true)
}
