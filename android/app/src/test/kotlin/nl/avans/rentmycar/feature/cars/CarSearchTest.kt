package nl.avans.rentmycar.feature.cars

import org.junit.Assert.assertEquals
import org.junit.Test

class CarSearchTest {
    private val polo = testCar(1, "Volkswagen", "Polo")
    private val tesla = testCar(2, "Tesla", "Model 3", CarCategory.BEV)
    private val cars = listOf(polo, tesla)

    @Test
    fun `search matches brand or model ignoring case`() {
        val result = CarSearch.find(cars, "TESLA", null)

        assertEquals(listOf(2), result.map { it.car.id })
    }

    @Test
    fun `every word of the search must match`() {
        val result = CarSearch.find(cars, "tesla polo", null)

        assertEquals(emptyList<Int>(), result.map { it.car.id })
    }
}
