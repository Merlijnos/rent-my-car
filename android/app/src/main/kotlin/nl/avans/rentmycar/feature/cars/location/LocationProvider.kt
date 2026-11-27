package nl.avans.rentmycar.feature.cars.location

/** Gives the phone's position. A small interface so ViewModels can be tested with a fake. */
interface LocationProvider {
    /** The current position, or null when the permission is missing or the phone has no position. */
    suspend fun currentLocation(): GeoPoint?
}
