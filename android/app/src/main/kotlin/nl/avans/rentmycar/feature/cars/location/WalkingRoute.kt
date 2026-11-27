package nl.avans.rentmycar.feature.cars.location

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

private const val GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps"

/**
 * Opens a walking route to [destination] in Google Maps. Without a start point Maps begins at the phone's position.
 * Falls back to the web version of Maps, and returns false when nothing on the phone can open a route.
 */
fun Context.openWalkingRoute(destination: GeoPoint): Boolean {
    val coordinates = "${destination.latitude},${destination.longitude}"
    val mapsApp = Intent(Intent.ACTION_VIEW, "google.navigation:q=$coordinates&mode=w".toUri())
        .setPackage(GOOGLE_MAPS_PACKAGE)
    val webPage = Intent(
        Intent.ACTION_VIEW,
        "https://www.google.com/maps/dir/?api=1&destination=$coordinates&travelmode=walking".toUri(),
    )
    return listOf(mapsApp, webPage).any { intent ->
        try {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }
}
