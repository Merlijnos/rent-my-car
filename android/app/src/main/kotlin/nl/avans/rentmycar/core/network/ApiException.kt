package nl.avans.rentmycar.core.network

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable

/** Error body of the backend: `{"error": "..."}`. */
@Serializable
data class ErrorResponse(val error: String)

class ApiException(
    val statusCode: Int,
    val serverMessage: String?,
) : Exception(serverMessage ?: "HTTP $statusCode")

/** Runs a network call and returns its failure as a [Result], so cancellation still cancels. */
suspend fun <T> apiCall(block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
