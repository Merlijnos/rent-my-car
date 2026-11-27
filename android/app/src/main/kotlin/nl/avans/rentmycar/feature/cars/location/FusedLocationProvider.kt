package nl.avans.rentmycar.feature.cars.location

import android.content.Context
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

private const val MAX_AGE_MILLIS = 5 * 60_000L
private const val TIMEOUT_MILLIS = 10_000L

/** [LocationProvider] on top of Google's Fused Location Provider (play-services-location). */
class FusedLocationProvider(private val context: Context) : LocationProvider {
    private val client = LocationServices.getFusedLocationProviderClient(context)

    override suspend fun currentLocation(): GeoPoint? {
        if (!context.hasLocationPermission()) return null
        return suspendCancellableCoroutine { continuation ->
            val request = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                .setMaxUpdateAgeMillis(MAX_AGE_MILLIS)
                .setDurationMillis(TIMEOUT_MILLIS)
                .build()
            val cancellation = CancellationTokenSource()
            continuation.invokeOnCancellation { cancellation.cancel() }
            @Suppress("MissingPermission") // checked above
            client.getCurrentLocation(request, cancellation.token)
                .addOnSuccessListener { location ->
                    continuation.resume(location?.let { GeoPoint(it.latitude, it.longitude) })
                }
                .addOnFailureListener { continuation.resume(null) }
        }
    }
}
