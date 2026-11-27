package nl.avans.rentmycar.feature.cars

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import nl.avans.rentmycar.R
import nl.avans.rentmycar.core.ui.theme.RentMyCarTheme
import nl.avans.rentmycar.feature.cars.location.GeoPoint
import org.junit.Rule
import org.junit.Test

class CarFormScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun showForm(initial: CarFormUiState) {
        composeRule.setContent {
            var form by remember { mutableStateOf(initial) }
            RentMyCarTheme {
                CarFormContent(
                    uiState = form,
                    onCategoryChange = {},
                    onFuelTypeChange = {},
                    onBrandChange = {},
                    onModelChange = {},
                    onPricePerDaypartChange = {},
                    onFixedCostsPerYearChange = {},
                    onConsumptionPer100KmChange = {},
                    onAddressChange = {},
                    onFindAddress = {},
                    onUseMyLocation = {},
                    onSave = { form = form.copy(errors = CarFormValidator.validate(form).errors) },
                )
            }
        }
    }

    @Test
    fun savingAnEmptyFormShowsTheBrandError() {
        showForm(CarFormUiState())

        composeRule.onNodeWithText(testString(R.string.form_save)).performScrollTo().performClick()

        composeRule.onNodeWithText(testString(R.string.form_error_brand)).assertIsDisplayed()
    }

    @Test
    fun foundAddressIsShownAsAPlaceName() {
        showForm(CarFormUiState(location = GeoPoint(51.5885, 4.7761), placeName = "Breda, Nederland"))

        composeRule.onNodeWithText(testString(R.string.form_location_found, "Breda, Nederland"))
            .performScrollTo()
            .assertIsDisplayed()
    }
}
