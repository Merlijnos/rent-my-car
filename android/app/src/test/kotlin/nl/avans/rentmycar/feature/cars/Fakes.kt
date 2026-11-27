package nl.avans.rentmycar.feature.cars

import nl.avans.rentmycar.core.network.ApiException
import nl.avans.rentmycar.feature.cars.location.AddressLookup
import nl.avans.rentmycar.feature.cars.location.AddressResult
import nl.avans.rentmycar.feature.cars.location.GeoPoint
import nl.avans.rentmycar.feature.cars.location.LocationProvider

fun testCar(
    id: Int,
    brand: String = "Volkswagen",
    model: String = "Polo",
    category: CarCategory = CarCategory.ICE,
    pricePerDaypart: Double = 15.0,
    ownerId: Int = 1,
    location: GeoPoint? = null,
) = Car(
    id = id,
    category = category,
    brand = brand,
    model = model,
    pricePerDaypart = pricePerDaypart,
    fixedCostsPerYear = 2400.0,
    consumptionPer100Km = 5.4,
    fuelType = if (category == CarCategory.ICE) FuelType.PETROL else null,
    ownerId = ownerId,
    latitude = location?.latitude,
    longitude = location?.longitude,
)

class FakeLocationProvider(var location: GeoPoint? = null) : LocationProvider {
    var calls = 0

    override suspend fun currentLocation(): GeoPoint? {
        calls++
        return location
    }
}

class FakeAddressLookup(
    var result: AddressResult = AddressResult.NotFound,
    var placeName: String? = null,
) : AddressLookup {
    var lastAddress: String? = null

    override suspend fun find(address: String): AddressResult {
        lastAddress = address
        return result
    }

    override suspend fun placeNameOf(point: GeoPoint): String? = placeName
}

/** Mimics the backend: filters on category and max price, and keeps the cars in memory. */
class FakeCarRepository(cars: List<Car> = emptyList()) : CarRepository {
    val cars = cars.toMutableList()
    var failure: Throwable? = null
    var lastCategory: CarCategory? = null
    var lastMaxPrice: Double? = null
    var lastKmPerYear: Int? = null
    var lastRequest: CarRequest? = null
    var deleteFailure: Throwable? = null

    private fun <T> result(block: () -> T): Result<T> = failure?.let { Result.failure(it) } ?: Result.success(block())

    override suspend fun getCars(category: CarCategory?, maxPricePerDaypart: Double?): Result<List<Car>> {
        lastCategory = category
        lastMaxPrice = maxPricePerDaypart
        return result {
            cars.filter { (category == null || it.category == category) && (maxPricePerDaypart == null || it.pricePerDaypart <= maxPricePerDaypart) }
        }
    }

    override suspend fun getCar(id: Int): Result<Car> {
        val car = cars.firstOrNull { it.id == id }
        return failure?.let { Result.failure(it) }
            ?: if (car != null) Result.success(car) else Result.failure(ApiException(404, "Car $id not found"))
    }

    override suspend fun getCosts(id: Int, kmPerYear: Int): Result<CarCosts> {
        lastKmPerYear = kmPerYear
        return result { CarCosts(id, kmPerYear, 0.1, 2400.0 + 0.1 * kmPerYear) }
    }

    override suspend fun createCar(request: CarRequest): Result<Car> {
        lastRequest = request
        return result { testCar(id = 100, brand = request.brand, model = request.model) }
    }

    override suspend fun updateCar(id: Int, request: CarRequest): Result<Car> {
        lastRequest = request
        return result { testCar(id = id, brand = request.brand, model = request.model) }
    }

    override suspend fun deleteCar(id: Int): Result<Unit> =
        deleteFailure?.let { Result.failure(it) } ?: result { cars.removeAll { it.id == id } }.map { }
}
