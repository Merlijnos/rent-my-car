package nl.avans.rentmycar.core.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import nl.avans.rentmycar.R
import nl.avans.rentmycar.core.network.ApiException
import java.io.IOException

/** A message for the user: a string resource plus an optional text to fill in. */
data class UserMessage(@param:StringRes val resId: Int, val argument: String? = null)

@Composable
fun UserMessage.asString(): String =
    if (argument != null) stringResource(resId, argument) else stringResource(resId)

/** The one place that turns a failed call into text the user can read. */
fun Throwable.toUserMessage(): UserMessage =
    when (this) {
        is ApiException -> toUserMessage()
        is IOException -> UserMessage(R.string.error_no_connection)
        else -> UserMessage(R.string.error_unknown)
    }

private fun ApiException.toUserMessage(): UserMessage =
    if (serverMessage != null) {
        UserMessage(R.string.error_server_message, serverMessage)
    } else {
        UserMessage(
            when (statusCode) {
                401 -> R.string.error_unauthorized
                403 -> R.string.error_forbidden
                404 -> R.string.error_not_found
                in 500..599 -> R.string.error_server
                else -> R.string.error_unknown
            },
        )
    }
