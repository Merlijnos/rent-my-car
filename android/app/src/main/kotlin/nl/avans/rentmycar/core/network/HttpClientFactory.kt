package nl.avans.rentmycar.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import nl.avans.rentmycar.core.session.Session

val appJson = Json { ignoreUnknownKeys = true }

/**
 * Builds the one HttpClient of the app. It sends the Bearer token of [session] on every request,
 * turns non-2xx responses into [ApiException] and logs out when an authenticated call gets a 401.
 */
fun createHttpClient(engine: HttpClientEngine, baseUrl: String, session: Session): HttpClient =
    HttpClient(engine) {
        install(ContentNegotiation) { json(appJson) }

        defaultRequest {
            url(baseUrl)
            session.token?.let { bearerAuth(it) }
        }

        HttpResponseValidator {
            validateResponse { response ->
                if (!response.status.isSuccess()) {
                    if (response.status == HttpStatusCode.Unauthorized && session.token != null) {
                        session.signOut()
                    }
                    throw ApiException(response.status.value, response.readErrorMessage())
                }
            }
        }
    }

private suspend fun HttpResponse.readErrorMessage(): String? =
    try {
        appJson.decodeFromString<ErrorResponse>(bodyAsText()).error
    } catch (_: SerializationException) {
        null
    }
