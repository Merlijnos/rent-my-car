package nl.avans.rentmycar.feature.cars

import nl.avans.rentmycar.feature.cars.location.DistanceCalculator
import nl.avans.rentmycar.feature.cars.location.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Test

class DistanceCalculatorTest {
    private val amsterdam = GeoPoint(52.3676, 4.9041)
    private val rotterdam = GeoPoint(51.9244, 4.4777)

    @Test
    fun `same point is zero kilometers`() {
        val distance = DistanceCalculator.distanceKm(amsterdam, amsterdam)

        assertEquals(0.0, distance, 0.0001)
    }

    @Test
    fun `amsterdam to rotterdam is about 57 kilometers`() {
        val distance = DistanceCalculator.distanceKm(amsterdam, rotterdam)

        assertEquals(57.2, distance, 0.5)
    }
}
