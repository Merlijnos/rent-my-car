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
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.hasAnyAncestor
import nl.avans.rentmycar.R
import nl.avans.rentmycar.core.ui.theme.RentMyCarTheme
import nl.avans.rentmycar.feature.cars.location.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CarDetailScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val car = sampleCar(id = 7, brand = "Tesla", model = "Model 3", category = CarCategory.BEV, ownerId = 1)

    private fun showDetail(
        initial: CarDetailUiState,
        onDeleteConfirm: () -> Unit = {},
        onRoute: () -> Unit = {},
    ) {
        composeRule.setContent {
            var uiState by remember { mutableStateOf(initial) }
            RentMyCarTheme {
                CarDetailContent(
                    uiState = uiState,
                    onKmPerYearChange = {},
                    onCalculate = {},
                    onRoute = onRoute,
                    onEdit = {},
                    onDeleteClick = { uiState = uiState.copy(showDeleteDialog = true) },
                    onDeleteConfirm = onDeleteConfirm,
                    onDeleteDismiss = { uiState = uiState.copy(showDeleteDialog = false) },
                    onRetry = {},
                )
            }
        }
    }

    @Test
    fun ownerSeesEditAndDeleteButtons() {
        showDetail(CarDetailUiState(car = car, isLoading = false, currentUserId = 1))

        composeRule.onNodeWithText(testString(R.string.detail_edit)).assertIsDisplayed()
        composeRule.onNodeWithText(testString(R.string.detail_delete)).assertIsDisplayed()
    }

    @Test
    fun otherUserDoesNotSeeEditAndDeleteButtons() {
        showDetail(CarDetailUiState(car = car, isLoading = false, currentUserId = 2))

        composeRule.onNodeWithText("Tesla Model 3").assertIsDisplayed()
        composeRule.onAllNodesWithText(testString(R.string.detail_edit)).assertCountEquals(0)
        composeRule.onAllNodesWithText(testString(R.string.detail_delete)).assertCountEquals(0)
    }

    @Test
    fun deleteAsksForConfirmationAndCancelClosesTheDialog() {
        showDetail(CarDetailUiState(car = car, isLoading = false, currentUserId = 1))

        composeRule.onNodeWithText(testString(R.string.detail_delete)).performClick()
        composeRule.onNodeWithText(testString(R.string.delete_dialog_title)).assertIsDisplayed()

        composeRule.onNodeWithText(testString(R.string.cancel)).performClick()
        composeRule.onAllNodesWithText(testString(R.string.delete_dialog_title)).assertCountEquals(0)
    }

    @Test
    fun confirmingTheDialogDeletesTheCar() {
        var confirmed = 0
        showDetail(CarDetailUiState(car = car, isLoading = false, currentUserId = 1), onDeleteConfirm = { confirmed++ })

        composeRule.onNodeWithText(testString(R.string.detail_delete)).performClick()
        composeRule.onNode(hasText(testString(R.string.detail_delete)) and hasAnyAncestor(isDialog())).performClick()

        assertEquals(1, confirmed)
    }

    @Test
    fun costsAreShownAfterCalculating() {
        val costs = CarCosts(carId = 7, kmPerYear = 15000, costPerKm = 0.25, totalCostOfOwnership = 3750.0)
        showDetail(CarDetailUiState(car = car, isLoading = false, currentUserId = 2, costs = costs))

        composeRule.onNodeWithText(testString(R.string.detail_cost_per_km_value, 0.25)).assertIsDisplayed()
    }

    @Test
    fun routeButtonIsShownForACarWithALocation() {
        val located = sampleCar(id = 8, brand = "Kia", model = "Niro", location = GeoPoint(51.5885, 4.7761))
        showDetail(CarDetailUiState(car = located, isLoading = false, currentUserId = 2))

        composeRule.onNodeWithText(testString(R.string.detail_route)).assertIsDisplayed()
    }

    @Test
    fun routeButtonIsHiddenForACarWithoutALocation() {
        showDetail(CarDetailUiState(car = car, isLoading = false, currentUserId = 2))

        composeRule.onAllNodesWithText(testString(R.string.detail_route)).assertCountEquals(0)
    }

    @Test
    fun routeButtonReportsTheClick() {
        var clicked = false
        val located = sampleCar(id = 8, brand = "Kia", model = "Niro", location = GeoPoint(51.5885, 4.7761))
        showDetail(CarDetailUiState(car = located, isLoading = false, currentUserId = 2), onRoute = { clicked = true })

        composeRule.onNodeWithText(testString(R.string.detail_route)).performClick()

        assertEquals(true, clicked)
    }
}
