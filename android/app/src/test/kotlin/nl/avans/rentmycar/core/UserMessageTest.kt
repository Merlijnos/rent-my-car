package nl.avans.rentmycar.core

import nl.avans.rentmycar.R
import nl.avans.rentmycar.core.common.UserMessage
import nl.avans.rentmycar.core.common.toUserMessage
import nl.avans.rentmycar.core.network.ApiException
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class UserMessageTest {
    @Test
    fun serverMessageIsShownAsIs() {
        val message = ApiException(400, "Car has active reservations").toUserMessage()

        assertEquals(UserMessage(R.string.error_server_message, "Car has active reservations"), message)
    }

    @Test
    fun statusWithoutMessageMapsToFixedText() {
        assertEquals(R.string.error_not_found, ApiException(404, null).toUserMessage().resId)
        assertEquals(R.string.error_forbidden, ApiException(403, null).toUserMessage().resId)
        assertEquals(R.string.error_server, ApiException(503, null).toUserMessage().resId)
    }

    @Test
    fun ioProblemMeansNoConnection() {
        assertEquals(R.string.error_no_connection, IOException("timeout").toUserMessage().resId)
    }

    @Test
    fun anythingElseIsUnknown() {
        assertEquals(R.string.error_unknown, IllegalStateException().toUserMessage().resId)
    }
}
