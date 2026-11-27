package nl.avans.rentmycar.feature.cars

import nl.avans.rentmycar.R
import nl.avans.rentmycar.feature.cars.location.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CarFormValidatorTest {
    private val valid = CarFormUiState(
        brand = " Volkswagen ",
        model = "Polo",
        pricePerDaypart = "15,50",
        fixedCostsPerYear = "2400",
        consumptionPer100Km = "5.4",
    )

    @Test
    fun validFormGivesATrimmedRequestWithoutErrors() {
        val result = CarFormValidator.validate(valid)

        assertEquals(false, result.errors.hasErrors)
        val request = result.request!!
        assertEquals("Volkswagen", request.brand)
        assertEquals(15.5, request.pricePerDaypart, 0.0)
        assertEquals(FuelType.PETROL, request.fuelType)
        assertNull(request.latitude)
    }

    @Test
    fun electricCarsHaveNoFuelType() {
        val request = CarFormValidator.validate(valid.copy(category = CarCategory.BEV)).request!!

        assertNull(request.fuelType)
    }

    @Test
    fun blankNamesAndBadNumbersGiveErrorsAndNoRequest() {
        val result = CarFormValidator.validate(valid.copy(brand = " ", pricePerDaypart = "abc", consumptionPer100Km = "0"))

        assertNull(result.request)
        assertEquals(R.string.form_error_brand, result.errors.brand)
        assertEquals(R.string.form_error_amount, result.errors.pricePerDaypart)
        assertEquals(R.string.form_error_consumption, result.errors.consumptionPer100Km)
        assertNull(result.errors.model)
    }

    @Test
    fun locationBecomesLatitudeAndLongitudeInTheRequest() {
        val request = CarFormValidator.validate(valid.copy(location = GeoPoint(51.5, 4.7))).request!!

        assertEquals(51.5, request.latitude!!, 0.0)
        assertEquals(4.7, request.longitude!!, 0.0)
    }
}
