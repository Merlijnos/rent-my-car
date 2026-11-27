package nl.avans.rentmycar.feature.cars

import androidx.test.platform.app.InstrumentationRegistry
import nl.avans.rentmycar.feature.cars.location.GeoPoint

fun testString(resId: Int, vararg args: Any): String =
    InstrumentationRegistry.getInstrumentation().targetContext.getString(resId, *args)

fun sampleCar(
    id: Int,
    brand: String,
    model: String,
    ownerId: Int = 1,
    location: GeoPoint? = null,
) = Car(
    id = id,
    category = CarCategory.ICE,
    brand = brand,
    model = model,
    pricePerDaypart = 15.0,
    fixedCostsPerYear = 2400.0,
    consumptionPer100Km = 5.4,
    fuelType = FuelType.PETROL,
    ownerId = ownerId,
    latitude = location?.latitude,
    longitude = location?.longitude,
)
