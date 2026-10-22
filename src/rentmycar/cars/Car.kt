package rentmycar.cars

enum class CarCategory { ICE, BEV, FCEV }

/** Fuel for an [IceCar], with its price per liter in euros. */
enum class FuelType(val pricePerLiter: Double) {
    PETROL(1.95),
    DIESEL(1.70),
    LPG(0.85),
}

/**
 * A rentable car. Each subclass calculates [costPerKm] with its own energy price.
 *
 * [consumptionPer100Km] is in liters (ICE), kWh (BEV) or kg of hydrogen (FCEV).
 * [id] is 0 until the car is stored.
 */
abstract class Car(
    val brand: String,
    val model: String,
    val pricePerDaypart: Double,
    val fixedCostsPerYear: Double,
    val consumptionPer100Km: Double,
    val ownerId: Int,
    val id: Int,
) {
    init {
        require(brand.isNotBlank() && brand.length <= 50) { "brand must be 1 to 50 characters" }
        require(model.isNotBlank() && model.length <= 50) { "model must be 1 to 50 characters" }
        require(pricePerDaypart >= 0) { "pricePerDaypart must not be negative" }
        require(fixedCostsPerYear >= 0) { "fixedCostsPerYear must not be negative" }
        require(consumptionPer100Km > 0) { "consumptionPer100Km must be greater than 0" }
    }

    abstract val category: CarCategory

    abstract fun costPerKm(): Double

    fun totalCostOfOwnership(kmPerYear: Int): Double {
        require(kmPerYear >= 0) { "kmPerYear must not be negative" }
        return fixedCostsPerYear + costPerKm() * kmPerYear
    }

    companion object {
        fun create(
            category: CarCategory,
            brand: String,
            model: String,
            pricePerDaypart: Double,
            fixedCostsPerYear: Double,
            consumptionPer100Km: Double,
            fuelType: FuelType?,
            ownerId: Int = 0,
            id: Int = 0,
        ): Car =
            when (category) {
                CarCategory.ICE -> {
                    val fuel = requireNotNull(fuelType) { "fuelType is required for an ICE car" }
                    IceCar(brand, model, pricePerDaypart, fixedCostsPerYear, consumptionPer100Km, fuel, ownerId, id)
                }
                CarCategory.BEV ->
                    BevCar(brand, model, pricePerDaypart, fixedCostsPerYear, consumptionPer100Km, ownerId, id)
                CarCategory.FCEV ->
                    FcevCar(brand, model, pricePerDaypart, fixedCostsPerYear, consumptionPer100Km, ownerId, id)
            }
    }
}

/** A car with an internal combustion engine. */
class IceCar(
    brand: String,
    model: String,
    pricePerDaypart: Double,
    fixedCostsPerYear: Double,
    consumptionPer100Km: Double,
    val fuelType: FuelType,
    ownerId: Int = 0,
    id: Int = 0,
) : Car(brand, model, pricePerDaypart, fixedCostsPerYear, consumptionPer100Km, ownerId, id) {
    override val category = CarCategory.ICE

    override fun costPerKm(): Double = fuelType.pricePerLiter * consumptionPer100Km / 100
}

/** A battery electric car. */
class BevCar(
    brand: String,
    model: String,
    pricePerDaypart: Double,
    fixedCostsPerYear: Double,
    consumptionPer100Km: Double,
    ownerId: Int = 0,
    id: Int = 0,
) : Car(brand, model, pricePerDaypart, fixedCostsPerYear, consumptionPer100Km, ownerId, id) {
    override val category = CarCategory.BEV

    override fun costPerKm(): Double = PRICE_PER_KWH * consumptionPer100Km / 100

    companion object {
        const val PRICE_PER_KWH = 0.35
    }
}

/** A hydrogen fuel cell car. */
class FcevCar(
    brand: String,
    model: String,
    pricePerDaypart: Double,
    fixedCostsPerYear: Double,
    consumptionPer100Km: Double,
    ownerId: Int = 0,
    id: Int = 0,
) : Car(brand, model, pricePerDaypart, fixedCostsPerYear, consumptionPer100Km, ownerId, id) {
    override val category = CarCategory.FCEV

    override fun costPerKm(): Double = PRICE_PER_KG * consumptionPer100Km / 100

    companion object {
        const val PRICE_PER_KG = 15.95
    }
}
