package nl.avans.rentmycar.feature.cars

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import nl.avans.rentmycar.R
import nl.avans.rentmycar.core.common.UserMessage
import nl.avans.rentmycar.core.ui.theme.RentMyCarTheme
import nl.avans.rentmycar.feature.cars.location.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CarListScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val cars = listOf(
        sampleCar(1, "Volkswagen", "Polo"),
        sampleCar(2, "Tesla", "Model 3", CarCategory.BEV),
        sampleCar(3, "Kia", "Niro EV", CarCategory.BEV),
    )

    private fun showList(
        initial: CarListUiState,
        onRetry: () -> Unit = {},
        onCarClick: (Int) -> Unit = {},
        onRequestLocation: () -> Unit = {},
        onOpenAppSettings: () -> Unit = {},
    ) {
        composeRule.setContent {
            var uiState by remember { mutableStateOf(initial) }
            RentMyCarTheme {
                CarListContent(
                    uiState = uiState,
                    onQueryChange = { uiState = uiState.copy(query = it) },
                    onCategoryChange = {},
                    onMaxPriceChange = {},
                    onSortByDistanceChange = {},
                    onRequestLocation = onRequestLocation,
                    onOpenAppSettings = onOpenAppSettings,
                    onRetry = onRetry,
                    onCarClick = onCarClick,
                    onAddCar = {},
                )
            }
        }
    }

    @Test
    fun listShowsAllCars() {
        showList(CarListUiState(cars = cars, isLoading = false))

        composeRule.onNodeWithText("Volkswagen Polo").assertIsDisplayed()
        composeRule.onNodeWithText("Tesla Model 3").assertIsDisplayed()
        composeRule.onNodeWithText("Kia Niro EV").assertIsDisplayed()
    }

    @Test
    fun searchFiltersTheCars() {
        showList(CarListUiState(cars = cars, isLoading = false))

        composeRule.onNodeWithText(testString(R.string.cars_search_label)).performTextInput("tesla")

        composeRule.onNodeWithText("Tesla Model 3").assertIsDisplayed()
        composeRule.onAllNodesWithText("Volkswagen Polo").assertCountEquals(0)
        composeRule.onAllNodesWithText("Kia Niro EV").assertCountEquals(0)
    }

    @Test
    fun searchWithoutMatchShowsMessage() {
        showList(CarListUiState(cars = cars, isLoading = false))

        composeRule.onNodeWithText(testString(R.string.cars_search_label)).performTextInput("zzz")

        composeRule.onNodeWithText(testString(R.string.cars_no_match)).assertIsDisplayed()
    }

    @Test
    fun errorStateShowsRetryButton() {
        var retries = 0
        showList(
            CarListUiState(isLoading = false, error = UserMessage(R.string.error_no_connection)),
            onRetry = { retries++ },
        )

        composeRule.onNodeWithText(testString(R.string.error_no_connection)).assertIsDisplayed()
        composeRule.onNodeWithText(testString(R.string.cars_retry)).performClick()

        assertEquals(1, retries)
    }

    @Test
    fun clickingACarReportsItsId() {
        var clickedId: Int? = null
        showList(CarListUiState(cars = cars, isLoading = false), onCarClick = { clickedId = it })

        composeRule.onNodeWithText("Tesla Model 3").performClick()

        assertEquals(2, clickedId)
    }

    @Test
    fun addButtonIsOnlyShownToOwners() {
        showList(CarListUiState(cars = cars, isLoading = false, canAddCars = false))
        composeRule.onAllNodesWithContentDescription(testString(R.string.cars_add)).assertCountEquals(0)
    }

    @Test
    fun addButtonIsShownToOwners() {
        showList(CarListUiState(cars = cars, isLoading = false, canAddCars = true))
        composeRule.onNodeWithContentDescription(testString(R.string.cars_add)).assertIsDisplayed()
    }

    private val breda = GeoPoint(51.5719, 4.7683)
    private val farCar = sampleCar(1, "Volkswagen", "Polo", location = GeoPoint(51.6978, 5.3037))
    private val nearCar = sampleCar(2, "Tesla", "Model 3", CarCategory.BEV, location = GeoPoint(51.5800, 4.7700))

    private fun topOf(text: String) = composeRule.onNodeWithText(text).fetchSemanticsNode().boundsInRoot.top

    @Test
    fun sortByDistanceShowsTheNearestCarFirstWithItsDistance() {
        showList(
            CarListUiState(
                cars = listOf(farCar, nearCar),
                isLoading = false,
                userLocation = breda,
                locationStatus = LocationStatus.AVAILABLE,
                sortByDistance = true,
            ),
        )

        assertTrue(topOf("Tesla Model 3") < topOf("Volkswagen Polo"))
        composeRule.onNodeWithText(testString(R.string.car_distance_away, 0.9)).assertIsDisplayed()
        composeRule.onNodeWithText(testString(R.string.car_distance_away, 39.5)).assertIsDisplayed()
    }

    @Test
    fun withoutALocationNoDistanceIsShown() {
        showList(CarListUiState(cars = listOf(farCar, nearCar), isLoading = false))

        composeRule.onAllNodesWithText(testString(R.string.car_distance_away, 0.9)).assertCountEquals(0)
    }

    @Test
    fun deniedPermissionShowsTheBannerAndStillListsTheCars() {
        var openedSettings = false
        showList(
            CarListUiState(cars = cars, isLoading = false, locationStatus = LocationStatus.DENIED),
            onOpenAppSettings = { openedSettings = true },
        )

        composeRule.onNodeWithText(testString(R.string.location_denied)).assertIsDisplayed()
        composeRule.onNodeWithText("Tesla Model 3").assertIsDisplayed()
        composeRule.onNodeWithText(testString(R.string.location_app_settings)).performClick()

        assertTrue(openedSettings)
    }

    @Test
    fun rationaleBannerAsksForLocation() {
        var asked = false
        showList(CarListUiState(cars = cars, isLoading = false), onRequestLocation = { asked = true })

        composeRule.onNodeWithText(testString(R.string.location_rationale_title)).assertIsDisplayed()
        composeRule.onNodeWithText(testString(R.string.location_allow)).performClick()

        assertTrue(asked)
    }
}
