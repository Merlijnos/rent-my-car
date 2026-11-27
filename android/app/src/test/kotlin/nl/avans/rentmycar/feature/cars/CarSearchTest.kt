package nl.avans.rentmycar.feature.cars

import nl.avans.rentmycar.feature.cars.location.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CarSearchTest {
    private val breda = GeoPoint(51.5719, 4.7683)
    private val polo = testCar(1, "Volkswagen", "Polo", location = GeoPoint(51.6978, 5.3037))
    private val tesla = testCar(2, "Tesla", "Model 3", CarCategory.BEV, location = GeoPoint(51.5800, 4.7700))
    private val kia = testCar(3, "Kia", "Niro EV", CarCategory.BEV)
    private val cars = listOf(polo, tesla, kia)

    private fun ids(items: List<CarListItem>) = items.map { it.car.id }

    @Test
    fun emptyQueryKeepsAllCars() {
        assertEquals(listOf(1, 2, 3), ids(CarSearch.find(cars, "", null)))
    }

    @Test
    fun queryMatchesBrandOrModelIgnoringCase() {
        assertEquals(listOf(2), ids(CarSearch.find(cars, "TESLA", null)))
        assertEquals(listOf(3), ids(CarSearch.find(cars, "niro", null)))
    }

    @Test
    fun everyWordOfTheQueryMustMatch() {
        assertEquals(listOf(2), ids(CarSearch.find(cars, "tesla model", null)))
        assertEquals(emptyList<Int>(), ids(CarSearch.find(cars, "tesla polo", null)))
    }

    @Test
    fun distanceIsOnlySetWhenBothLocationsAreKnown() {
        val items = CarSearch.find(cars, "", breda)

        assertEquals(39.5, items[0].distanceKm!!, 0.5)
        assertEquals(0.9, items[1].distanceKm!!, 0.2)
        assertNull(items[2].distanceKm)
        assertNull(CarSearch.find(cars, "", null).first().distanceKm)
    }

    @Test
    fun sortByDistanceGivesNearestFirstAndUnknownLast() {
        val sorted = CarSearch.sortByDistance(CarSearch.find(cars, "", breda))

        assertEquals(listOf(2, 1, 3), ids(sorted))
    }
}
