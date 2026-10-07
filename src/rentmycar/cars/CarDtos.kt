package rentmycar.cars

import kotlin.math.round
import kotlinx.serialization.Serializable

@Serializable
data class CarRequest(
    val category: CarCategory,
    val brand: String,
    val model: String,
    val pricePerDaypart: Double,
    val fixedCostsPerYear: Double,
    val consumptionPer100Km: Double,
    val fuelType: FuelType? = null,
) {
    fun toCar(id: Int = 0): Car =
        Car.create(category, brand, model, pricePerDaypart, fixedCostsPerYear, consumptionPer100Km, fuelType, id)
}

@Serializable
data class CarResponse(
    val id: Int,
    val category: CarCategory,
    val brand: String,
    val model: String,
    val pricePerDaypart: Double,
    val fixedCostsPerYear: Double,
    val consumptionPer100Km: Double,
    val fuelType: FuelType?,
)

@Serializable
data class CarCostsResponse(
    val carId: Int,
    val kmPerYear: Int,
    val costPerKm: Double,
    val totalCostOfOwnership: Double,
)

fun Car.toResponse() =
    CarResponse(
        id = id,
        category = category,
        brand = brand,
        model = model,
        pricePerDaypart = pricePerDaypart,
        fixedCostsPerYear = fixedCostsPerYear,
        consumptionPer100Km = consumptionPer100Km,
        fuelType = (this as? IceCar)?.fuelType,
    )

fun Car.toCostsResponse(kmPerYear: Int) =
    CarCostsResponse(
        carId = id,
        kmPerYear = kmPerYear,
        costPerKm = round(costPerKm() * 10_000) / 10_000,
        totalCostOfOwnership = round(totalCostOfOwnership(kmPerYear) * 100) / 100,
    )
