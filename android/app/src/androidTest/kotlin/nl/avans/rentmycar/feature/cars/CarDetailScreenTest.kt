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
import nl.avans.rentmycar.R
import nl.avans.rentmycar.core.ui.theme.RentMyCarTheme
import org.junit.Rule
import org.junit.Test

class CarDetailScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val car = sampleCar(id = 7, brand = "Tesla", model = "Model 3", ownerId = 1)

    private fun showDetail(initial: CarDetailUiState) {
        composeRule.setContent {
            var uiState by remember { mutableStateOf(initial) }
            RentMyCarTheme {
                CarDetailContent(
                    uiState = uiState,
                    onKmPerYearChange = {},
                    onCalculate = {},
                    onRoute = {},
                    onEdit = {},
                    onDeleteClick = { uiState = uiState.copy(showDeleteDialog = true) },
                    onDeleteConfirm = {},
                    onDeleteDismiss = { uiState = uiState.copy(showDeleteDialog = false) },
                    onRetry = {},
                )
            }
        }
    }

    @Test
    fun otherUserDoesNotSeeEditAndDelete() {
        showDetail(CarDetailUiState(car = car, isLoading = false, currentUserId = 2))

        composeRule.onNodeWithText("Tesla Model 3").assertIsDisplayed()
        composeRule.onAllNodesWithText(testString(R.string.detail_edit)).assertCountEquals(0)
        composeRule.onAllNodesWithText(testString(R.string.detail_delete)).assertCountEquals(0)
    }

    @Test
    fun deleteAsksForConfirmationFirst() {
        showDetail(CarDetailUiState(car = car, isLoading = false, currentUserId = 1))

        composeRule.onNodeWithText(testString(R.string.detail_delete)).performClick()

        composeRule.onNodeWithText(testString(R.string.delete_dialog_title)).assertIsDisplayed()
    }
}
