package nl.avans.rentmycar.feature.cars.location

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** A position on Earth in degrees. */
data class GeoPoint(val latitude: Double, val longitude: Double)

/** Distance over the Earth's surface between two points, with the haversine formula. */
object DistanceCalculator {
    private const val EARTH_RADIUS_KM = 6371.0

    fun distanceKm(from: GeoPoint, to: GeoPoint): Double {
        val deltaLatitude = Math.toRadians(to.latitude - from.latitude)
        val deltaLongitude = Math.toRadians(to.longitude - from.longitude)
        val a = sin(deltaLatitude / 2).pow(2) +
            cos(Math.toRadians(from.latitude)) * cos(Math.toRadians(to.latitude)) *
            sin(deltaLongitude / 2).pow(2)
        return 2 * EARTH_RADIUS_KM * asin(sqrt(a))
    }
}
