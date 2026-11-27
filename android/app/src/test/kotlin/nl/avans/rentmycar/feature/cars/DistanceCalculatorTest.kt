package nl.avans.rentmycar.feature.cars

import nl.avans.rentmycar.feature.cars.location.DistanceCalculator
import nl.avans.rentmycar.feature.cars.location.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Test

class DistanceCalculatorTest {
    private val amsterdam = GeoPoint(52.3676, 4.9041)
    private val rotterdam = GeoPoint(51.9244, 4.4777)

    @Test
    fun samePointIsZeroKilometers() {
        assertEquals(0.0, DistanceCalculator.distanceKm(amsterdam, amsterdam), 0.0001)
    }

    @Test
    fun amsterdamToRotterdamIsAbout57Kilometers() {
        assertEquals(57.2, DistanceCalculator.distanceKm(amsterdam, rotterdam), 0.5)
    }

    @Test
    fun distanceIsTheSameInBothDirections() {
        assertEquals(
            DistanceCalculator.distanceKm(amsterdam, rotterdam),
            DistanceCalculator.distanceKm(rotterdam, amsterdam),
            0.0001,
        )
    }

    @Test
    fun oneDegreeOfLatitudeIsAbout111Kilometers() {
        assertEquals(111.2, DistanceCalculator.distanceKm(GeoPoint(0.0, 0.0), GeoPoint(1.0, 0.0)), 0.1)
    }

    @Test
    fun halfwayAroundTheEquatorIsHalfTheCircumference() {
        assertEquals(20015.1, DistanceCalculator.distanceKm(GeoPoint(0.0, 0.0), GeoPoint(0.0, 180.0)), 0.5)
    }
}
