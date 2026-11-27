package nl.avans.rentmycar.feature.cars

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import nl.avans.rentmycar.R
import nl.avans.rentmycar.core.common.UserMessage
import nl.avans.rentmycar.core.common.toUserMessage
import nl.avans.rentmycar.feature.cars.location.AddressLookup
import nl.avans.rentmycar.feature.cars.location.AddressResult
import nl.avans.rentmycar.feature.cars.location.GeoPoint
import nl.avans.rentmycar.feature.cars.location.LocationProvider

/** One error message (string resource) per field, or null when the field is fine. */
data class CarFormErrors(
    @param:StringRes val brand: Int? = null,
    @param:StringRes val model: Int? = null,
    @param:StringRes val pricePerDaypart: Int? = null,
    @param:StringRes val fixedCostsPerYear: Int? = null,
    @param:StringRes val consumptionPer100Km: Int? = null,
) {
    val hasErrors: Boolean
        get() = listOf(brand, model, pricePerDaypart, fixedCostsPerYear, consumptionPer100Km).any { it != null }
}

/**
 * The form fields stay text, so half-typed numbers like "1." do not get lost.
 * The car's [location] comes from an address search or the GPS, the owner never types coordinates.
 */
data class CarFormUiState(
    val category: CarCategory = CarCategory.ICE,
    val brand: String = "",
    val model: String = "",
    val pricePerDaypart: String = "",
    val fixedCostsPerYear: String = "",
    val consumptionPer100Km: String = "",
    val fuelType: FuelType = FuelType.PETROL,
    val address: String = "",
    val location: GeoPoint? = null,
    val placeName: String? = null,
    val errors: CarFormErrors = CarFormErrors(),
    val isLoading: Boolean = false,
    val loadError: UserMessage? = null,
    val isSaving: Boolean = false,
    val saveError: UserMessage? = null,
    val isLocating: Boolean = false,
    @param:StringRes val locationError: Int? = null,
    val isSaved: Boolean = false,
)

/** The outcome of checking the form: the [errors] to show, and the [request] to send when there are none. */
data class CarFormValidation(val errors: CarFormErrors, val request: CarRequest?)

/** Creates a car when [carId] is null, otherwise loads that car and updates it. */
class CarFormViewModel(
    private val carId: Int?,
    private val repository: CarRepository,
    private val locationProvider: LocationProvider,
    private val addressLookup: AddressLookup,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CarFormUiState())
    val uiState: StateFlow<CarFormUiState> = _uiState.asStateFlow()

    init {
        if (carId != null) load(carId)
    }

    fun load(id: Int) {
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            repository.getCar(id)
                .onSuccess { car ->
                    _uiState.update {
                        it.copy(
                            category = car.category,
                            brand = car.brand,
                            model = car.model,
                            pricePerDaypart = car.pricePerDaypart.toString(),
                            fixedCostsPerYear = car.fixedCostsPerYear.toString(),
                            consumptionPer100Km = car.consumptionPer100Km.toString(),
                            fuelType = car.fuelType ?: FuelType.PETROL,
                            location = car.location,
                            isLoading = false,
                        )
                    }
                    car.location?.let { point -> showPlaceName(point) }
                }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, loadError = e.toUserMessage()) } }
        }
    }

    fun onCategoryChange(category: CarCategory) = change { copy(category = category) }

    fun onFuelTypeChange(fuelType: FuelType) = change { copy(fuelType = fuelType) }

    fun onBrandChange(brand: String) = change { copy(brand = brand, errors = errors.copy(brand = null)) }

    fun onModelChange(model: String) = change { copy(model = model, errors = errors.copy(model = null)) }

    fun onPricePerDaypartChange(value: String) =
        change { copy(pricePerDaypart = value, errors = errors.copy(pricePerDaypart = null)) }

    fun onFixedCostsPerYearChange(value: String) =
        change { copy(fixedCostsPerYear = value, errors = errors.copy(fixedCostsPerYear = null)) }

    fun onConsumptionPer100KmChange(value: String) =
        change { copy(consumptionPer100Km = value, errors = errors.copy(consumptionPer100Km = null)) }

    fun onAddressChange(address: String) = change { copy(address = address, locationError = null) }

    /** Looks up the typed address and sets the car's location to it. */
    fun onFindAddress() {
        val address = _uiState.value.address.trim()
        if (address.isEmpty()) {
            change { copy(locationError = R.string.form_address_empty) }
            return
        }
        change { copy(isLocating = true, locationError = null) }
        viewModelScope.launch {
            when (val result = addressLookup.find(address)) {
                is AddressResult.Found ->
                    change { copy(isLocating = false, location = result.point, placeName = result.placeName) }
                AddressResult.NotFound -> change { copy(isLocating = false, locationError = R.string.form_address_not_found) }
                AddressResult.Failed -> change { copy(isLocating = false, locationError = R.string.form_address_failed) }
            }
        }
    }

    /** Sets the car's location to the phone's position. Call with true when the permission is granted. */
    fun onLocationPermissionResult(granted: Boolean) {
        if (!granted) {
            change { copy(locationError = R.string.form_location_denied) }
            return
        }
        change { copy(isLocating = true, locationError = null) }
        viewModelScope.launch {
            val location = locationProvider.currentLocation()
            change {
                if (location == null) {
                    copy(isLocating = false, locationError = R.string.form_location_unavailable)
                } else {
                    copy(isLocating = false, location = location, placeName = null)
                }
            }
            if (location != null) showPlaceName(location)
        }
    }

    private suspend fun showPlaceName(point: GeoPoint) {
        val name = addressLookup.placeNameOf(point)
        change { if (location == point) copy(placeName = name) else this }
    }

    fun save() {
        if (_uiState.value.isSaving) return
        val validation = CarFormValidator.validate(_uiState.value)
        _uiState.update { it.copy(errors = validation.errors, saveError = null) }
        val request = validation.request ?: return

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = if (carId == null) repository.createCar(request) else repository.updateCar(carId, request)
            result
                .onSuccess { _uiState.update { it.copy(isSaving = false, isSaved = true) } }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, saveError = e.toUserMessage()) } }
        }
    }

    private fun change(transform: CarFormUiState.() -> CarFormUiState) {
        _uiState.update { it.transform() }
    }
}
