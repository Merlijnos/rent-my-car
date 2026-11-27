package nl.avans.rentmycar.feature.cars

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import nl.avans.rentmycar.core.network.apiCall

/** Everything the car screens need from the backend. An interface so tests can use a fake. */
interface CarRepository {
    suspend fun getCars(category: CarCategory?, maxPricePerDaypart: Double?): Result<List<Car>>

    suspend fun getCar(id: Int): Result<Car>

    suspend fun getCosts(id: Int, kmPerYear: Int): Result<CarCosts>

    suspend fun createCar(request: CarRequest): Result<Car>

    suspend fun updateCar(id: Int, request: CarRequest): Result<Car>

    suspend fun deleteCar(id: Int): Result<Unit>
}

class ApiCarRepository(private val client: HttpClient) : CarRepository {
    override suspend fun getCars(category: CarCategory?, maxPricePerDaypart: Double?): Result<List<Car>> =
        apiCall {
            client.get("cars") {
                parameter("category", category?.name)
                parameter("maxPricePerDaypart", maxPricePerDaypart)
            }.body()
        }

    override suspend fun getCar(id: Int): Result<Car> =
        apiCall { client.get("cars/$id").body() }

    override suspend fun getCosts(id: Int, kmPerYear: Int): Result<CarCosts> =
        apiCall { client.get("cars/$id/costs") { parameter("kmPerYear", kmPerYear) }.body() }

    override suspend fun createCar(request: CarRequest): Result<Car> =
        apiCall {
            client.post("cars") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }

    override suspend fun updateCar(id: Int, request: CarRequest): Result<Car> =
        apiCall {
            client.put("cars/$id") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }

    override suspend fun deleteCar(id: Int): Result<Unit> =
        apiCall { client.delete("cars/$id") }.map { }
}
