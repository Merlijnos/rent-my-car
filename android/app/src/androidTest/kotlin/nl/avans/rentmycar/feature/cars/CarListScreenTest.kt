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
import androidx.compose.ui.test.performTextInput
import nl.avans.rentmycar.R
import nl.avans.rentmycar.core.ui.theme.RentMyCarTheme
import nl.avans.rentmycar.feature.cars.location.GeoPoint
import org.junit.Rule
import org.junit.Test

class CarListScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun showList(initial: CarListUiState) {
        composeRule.setContent {
            var uiState by remember { mutableStateOf(initial) }
            RentMyCarTheme {
                CarListContent(
                    uiState = uiState,
                    onQueryChange = { uiState = uiState.copy(query = it) },
                    onCategoryChange = {},
                    onMaxPriceChange = {},
                    onSortByDistanceChange = {},
                    onRequestLocation = {},
                    onOpenAppSettings = {},
                    onRetry = {},
                    onCarClick = {},
                    onAddCar = {},
                )
            }
        }
    }

    @Test
    fun listShowsTheCarsAndSearchingFiltersThem() {
        val cars = listOf(sampleCar(1, "Volkswagen", "Polo"), sampleCar(2, "Tesla", "Model 3"))
        showList(CarListUiState(cars = cars, isLoading = false))
        composeRule.onNodeWithText("Volkswagen Polo").assertIsDisplayed()
        composeRule.onNodeWithText("Tesla Model 3").assertIsDisplayed()

        composeRule.onNodeWithText(testString(R.string.cars_search_label)).performTextInput("tesla")

        composeRule.onNodeWithText("Tesla Model 3").assertIsDisplayed()
        composeRule.onAllNodesWithText("Volkswagen Polo").assertCountEquals(0)
    }

    @Test
    fun sortByDistanceShowsHowFarAwayACarIs() {
        val nearCar = sampleCar(1, "Tesla", "Model 3", location = GeoPoint(51.5800, 4.7700))
        showList(
            CarListUiState(
                cars = listOf(nearCar),
                isLoading = false,
                userLocation = GeoPoint(51.5719, 4.7683),
                locationStatus = LocationStatus.AVAILABLE,
                sortByDistance = true,
            ),
        )

        composeRule.onNodeWithText(testString(R.string.car_distance_away, 0.9)).assertIsDisplayed()
    }
}
