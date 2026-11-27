package nl.avans.rentmycar.feature.cars

import nl.avans.rentmycar.core.session.AuthSession
import nl.avans.rentmycar.core.session.SessionStorage
import nl.avans.rentmycar.feature.cars.location.AddressLookup
import nl.avans.rentmycar.feature.cars.location.AddressResult
import nl.avans.rentmycar.feature.cars.location.GeoPoint
import nl.avans.rentmycar.feature.cars.location.LocationProvider

fun testCar(
    id: Int,
    brand: String,
    model: String,
    category: CarCategory = CarCategory.ICE,
    location: GeoPoint? = null,
) = Car(
    id = id,
    category = category,
    brand = brand,
    model = model,
    pricePerDaypart = 15.0,
    fixedCostsPerYear = 2400.0,
    consumptionPer100Km = 5.4,
    fuelType = if (category == CarCategory.ICE) FuelType.PETROL else null,
    ownerId = 1,
    latitude = location?.latitude,
    longitude = location?.longitude,
)

class FakeSessionStorage : SessionStorage {
    override suspend fun load(): AuthSession? = null

    override suspend fun save(session: AuthSession) = Unit

    override suspend fun clear() = Unit
}

class FakeLocationProvider(private val location: GeoPoint?) : LocationProvider {
    override suspend fun currentLocation(): GeoPoint? = location
}

class FakeAddressLookup(private val result: AddressResult) : AddressLookup {
    override suspend fun find(address: String): AddressResult = result

    override suspend fun placeNameOf(point: GeoPoint): String? = null
}

class FakeCarRepository(private val cars: List<Car> = emptyList()) : CarRepository {
    var lastRequest: CarRequest? = null

    override suspend fun getCars(category: CarCategory?, maxPricePerDaypart: Double?) = Result.success(cars)

    override suspend fun getCar(id: Int): Result<Car> = error("not used in these tests")

    override suspend fun getCosts(id: Int, kmPerYear: Int): Result<CarCosts> = error("not used in these tests")

    override suspend fun createCar(request: CarRequest): Result<Car> {
        lastRequest = request
        return Result.success(testCar(100, request.brand, request.model))
    }

    override suspend fun updateCar(id: Int, request: CarRequest): Result<Car> = error("not used in these tests")

    override suspend fun deleteCar(id: Int): Result<Unit> = error("not used in these tests")
}
