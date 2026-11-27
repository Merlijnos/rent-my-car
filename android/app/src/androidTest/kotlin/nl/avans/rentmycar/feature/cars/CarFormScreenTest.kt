package nl.avans.rentmycar.feature.cars

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import nl.avans.rentmycar.R
import nl.avans.rentmycar.core.ui.theme.RentMyCarTheme
import nl.avans.rentmycar.feature.cars.location.GeoPoint
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CarFormScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    /** Same wiring as the real screen: Save runs the validation and puts the errors in the state. */
    private fun showForm(
        initial: CarFormUiState = CarFormUiState(),
        onFindAddress: () -> Unit = {},
        onUseMyLocation: () -> Unit = {},
    ) {
        composeRule.setContent {
            var form by remember { mutableStateOf(initial) }
            RentMyCarTheme {
                CarFormContent(
                    uiState = form,
                    onCategoryChange = { form = form.copy(category = it) },
                    onFuelTypeChange = { form = form.copy(fuelType = it) },
                    onBrandChange = { form = form.copy(brand = it) },
                    onModelChange = { form = form.copy(model = it) },
                    onPricePerDaypartChange = { form = form.copy(pricePerDaypart = it) },
                    onFixedCostsPerYearChange = { form = form.copy(fixedCostsPerYear = it) },
                    onConsumptionPer100KmChange = { form = form.copy(consumptionPer100Km = it) },
                    onAddressChange = { form = form.copy(address = it) },
                    onFindAddress = onFindAddress,
                    onUseMyLocation = onUseMyLocation,
                    onSave = { form = form.copy(errors = CarFormValidator.validate(form).errors) },
                )
            }
        }
    }

    @Test
    fun savingAnEmptyFormShowsErrorsOnTheRequiredFields() {
        showForm()

        composeRule.onNodeWithText(testString(R.string.form_save)).performScrollTo().performClick()

        composeRule.onNodeWithText(testString(R.string.form_error_brand)).assertIsDisplayed()
        composeRule.onNodeWithText(testString(R.string.form_error_model)).assertIsDisplayed()
    }

    @Test
    fun noErrorsAreShownBeforeSaving() {
        showForm()

        composeRule.onAllNodesWithText(testString(R.string.form_error_brand)).assertCountEquals(0)
    }

    @Test
    fun addressFieldAndFindButtonAreShownInsteadOfCoordinates() {
        showForm()

        composeRule.onNodeWithText(testString(R.string.form_address)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(testString(R.string.form_address_find)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun findButtonReportsTheClick() {
        var clicked = false
        showForm(onFindAddress = { clicked = true })

        composeRule.onNodeWithText(testString(R.string.form_address)).performScrollTo().performTextInput("Grote Markt, Breda")
        composeRule.onNodeWithText(testString(R.string.form_address_find)).performScrollTo().performClick()

        assertTrue(clicked)
    }

    @Test
    fun foundPlaceIsShownAsText() {
        showForm(CarFormUiState(location = GeoPoint(51.5885, 4.7761), placeName = "Breda, Nederland"))

        composeRule.onNodeWithText(testString(R.string.form_location_found, "Breda, Nederland"))
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun noPlaceIsShownBeforeSearching() {
        showForm()

        composeRule.onAllNodesWithText(testString(R.string.form_location_found, "")).assertCountEquals(0)
    }

    @Test
    fun addressNotFoundErrorIsShown() {
        showForm(CarFormUiState(locationError = R.string.form_address_not_found))

        composeRule.onNodeWithText(testString(R.string.form_address_not_found)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun fuelTypeIsOnlyAskedForCombustionCars() {
        showForm()
        composeRule.onNodeWithText(testString(R.string.form_fuel_type)).assertIsDisplayed()

        composeRule.onNodeWithText(CarCategory.BEV.name).performClick()

        composeRule.onAllNodesWithText(testString(R.string.form_fuel_type)).assertCountEquals(0)
    }

    @Test
    fun useMyLocationButtonReportsTheClick() {
        var clicked = false
        showForm(onUseMyLocation = { clicked = true })

        composeRule.onNodeWithText(testString(R.string.form_use_location)).performScrollTo().performClick()

        assertTrue(clicked)
    }

    @Test
    fun locationErrorIsShownNextToTheButton() {
        showForm(CarFormUiState(locationError = R.string.form_location_denied))

        composeRule.onNodeWithText(testString(R.string.form_location_denied)).performScrollTo().assertIsDisplayed()
    }
}
