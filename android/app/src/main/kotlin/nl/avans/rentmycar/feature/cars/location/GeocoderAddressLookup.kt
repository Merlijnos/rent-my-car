package nl.avans.rentmycar.feature.cars.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/** [AddressLookup] on top of Android's [Geocoder]. */
class GeocoderAddressLookup(context: Context) : AddressLookup {
    private val geocoder = Geocoder(context, Locale.getDefault())

    override suspend fun find(address: String): AddressResult {
        if (!Geocoder.isPresent()) return AddressResult.Failed
        return try {
            val match = addressesNamed(address).firstOrNull() ?: return AddressResult.NotFound
            AddressResult.Found(GeoPoint(match.latitude, match.longitude), match.placeName() ?: address.trim())
        } catch (e: IOException) {
            AddressResult.Failed
        }
    }

    override suspend fun placeNameOf(point: GeoPoint): String? {
        if (!Geocoder.isPresent()) return null
        return try {
            addressesAt(point).firstOrNull()?.placeName()
        } catch (e: IOException) {
            null
        }
    }

    // From Android 13 the Geocoder reports through a listener; older versions block, so they run on a background thread.
    private suspend fun addressesNamed(name: String): List<Address> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocationName(name, 1, ContinuationListener(continuation))
            }
        } else {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                geocoder.getFromLocationName(name, 1).orEmpty()
            }
        }

    private suspend fun addressesAt(point: GeoPoint): List<Address> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocation(point.latitude, point.longitude, 1, ContinuationListener(continuation))
            }
        } else {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(point.latitude, point.longitude, 1).orEmpty()
            }
        }

    /** Hands the Geocoder's answer, or its error, to the waiting coroutine. */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private class ContinuationListener(
        private val continuation: Continuation<List<Address>>,
    ) : Geocoder.GeocodeListener {
        override fun onGeocode(addresses: MutableList<Address>) = continuation.resume(addresses)

        override fun onError(errorMessage: String?) = continuation.resumeWithException(IOException(errorMessage))
    }
}

private fun Address.placeName(): String? =
    listOfNotNull(locality ?: subAdminArea ?: featureName, countryName).joinToString(", ").ifBlank { null }
