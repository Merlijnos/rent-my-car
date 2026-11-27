package nl.avans.rentmycar.feature.cars

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import nl.avans.rentmycar.R
import nl.avans.rentmycar.core.common.UserMessage
import nl.avans.rentmycar.core.ui.theme.RentMyCarTheme
import nl.avans.rentmycar.feature.cars.location.GeoPoint

private val previewLocation = GeoPoint(51.5719, 4.7683)

private val previewCars = listOf(
    Car(1, CarCategory.ICE, "Volkswagen", "Polo", 15.0, 2400.0, 5.4, FuelType.PETROL, 1, 51.6978, 5.3037),
    Car(2, CarCategory.BEV, "Tesla", "Model 3", 35.0, 3100.0, 15.2, null, 1, 51.58, 4.77),
    Car(3, CarCategory.FCEV, "Toyota", "Mirai", 45.0, 3600.0, 0.9, null, 2),
)

@Composable
private fun PreviewList(uiState: CarListUiState) {
    RentMyCarTheme {
        CarListContent(
            uiState = uiState,
            onQueryChange = {},
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

@Preview(showBackground = true, name = "Car list: loaded with distances")
@Composable
private fun CarListLoadedPreview() = PreviewList(
    CarListUiState(
        cars = previewCars,
        isLoading = false,
        userLocation = previewLocation,
        locationStatus = LocationStatus.AVAILABLE,
        sortByDistance = true,
        canAddCars = true,
    ),
)

@Preview(showBackground = true, name = "Car list: empty")
@Composable
private fun CarListEmptyPreview() = PreviewList(CarListUiState(isLoading = false))

@Preview(showBackground = true, name = "Car list: error")
@Composable
private fun CarListErrorPreview() =
    PreviewList(CarListUiState(isLoading = false, error = UserMessage(R.string.error_no_connection)))

@Preview(showBackground = true, name = "Car detail: owner")
@Composable
private fun CarDetailOwnerPreview() {
    RentMyCarTheme {
        CarDetailContent(
            uiState = CarDetailUiState(
                car = previewCars[1],
                isLoading = false,
                currentUserId = 1,
                userLocation = previewLocation,
                costs = CarCosts(carId = 2, kmPerYear = 10_000, costPerKm = 0.46, totalCostOfOwnership = 4600.0),
            ),
            onKmPerYearChange = {},
            onCalculate = {},
            onRoute = {},
            onEdit = {},
            onDeleteClick = {},
            onDeleteConfirm = {},
            onDeleteDismiss = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, name = "Car form: validation errors")
@Composable
private fun CarFormErrorsPreview() {
    val form = CarFormUiState(location = previewLocation, placeName = "Breda, Nederland")
    RentMyCarTheme {
        CarFormContent(
            uiState = form.copy(errors = CarFormValidator.validate(form).errors),
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
            onSave = {},
        )
    }
}
