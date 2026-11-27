package nl.avans.rentmycar.feature.cars.location

/** Turns an address or place name into a position and back. A small interface so ViewModels can be tested with a fake. */
interface AddressLookup {
    suspend fun find(address: String): AddressResult

    /** The place name for [point], or null when it cannot be found. */
    suspend fun placeNameOf(point: GeoPoint): String?
}

sealed interface AddressResult {
    data class Found(val point: GeoPoint, val placeName: String) : AddressResult

    data object NotFound : AddressResult

    /** The lookup itself failed, for example because the phone is offline. */
    data object Failed : AddressResult
}
