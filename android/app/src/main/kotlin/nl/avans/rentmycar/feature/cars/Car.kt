package nl.avans.rentmycar.feature.cars

import kotlinx.serialization.Serializable
import nl.avans.rentmycar.feature.cars.location.GeoPoint

@Serializable
enum class CarCategory { ICE, BEV, FCEV }

@Serializable
enum class FuelType { PETROL, DIESEL, LPG }

/** A car as the backend returns it. [latitude] and [longitude] are both set or both null. */
@Serializable
data class Car(
    val id: Int,
    val category: CarCategory,
    val brand: String,
    val model: String,
    val pricePerDaypart: Double,
    val fixedCostsPerYear: Double,
    val consumptionPer100Km: Double,
    val fuelType: FuelType? = null,
    val ownerId: Int,
    val latitude: Double? = null,
    val longitude: Double? = null,
) {
    val location: GeoPoint?
        get() = if (latitude != null && longitude != null) GeoPoint(latitude, longitude) else null
}

/** The body of POST /cars and PUT /cars/{id}. The owner is taken from the token, not from the body. */
@Serializable
data class CarRequest(
    val category: CarCategory,
    val brand: String,
    val model: String,
    val pricePerDaypart: Double,
    val fixedCostsPerYear: Double,
    val consumptionPer100Km: Double,
    val fuelType: FuelType? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

@Serializable
data class CarCosts(
    val carId: Int,
    val kmPerYear: Int,
    val costPerKm: Double,
    val totalCostOfOwnership: Double,
)
