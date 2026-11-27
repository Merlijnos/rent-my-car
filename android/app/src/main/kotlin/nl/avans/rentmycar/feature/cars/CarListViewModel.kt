package nl.avans.rentmycar.feature.cars

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import nl.avans.rentmycar.core.common.UserMessage
import nl.avans.rentmycar.core.common.toUserMessage
import nl.avans.rentmycar.core.session.Role
import nl.avans.rentmycar.core.session.Session
import nl.avans.rentmycar.feature.cars.location.GeoPoint
import nl.avans.rentmycar.feature.cars.location.LocationProvider

enum class LocationStatus { NOT_ASKED, LOADING, AVAILABLE, DENIED, UNAVAILABLE }

data class CarListItem(val car: Car, val distanceKm: Double?)

/**
 * [cars] is what the backend returned for the category and max price filter.
 * The search text and the sort order are applied on the phone, in [visibleCars].
 */
data class CarListUiState(
    val cars: List<Car> = emptyList(),
    val query: String = "",
    val category: CarCategory? = null,
    val maxPrice: String = "",
    val sortByDistance: Boolean = false,
    val userLocation: GeoPoint? = null,
    val locationStatus: LocationStatus = LocationStatus.NOT_ASKED,
    val isLoading: Boolean = true,
    val error: UserMessage? = null,
    val canAddCars: Boolean = false,
) {
    val maxPriceValue: Double?
        get() = maxPrice.toDecimalOrNull()

    val isMaxPriceInvalid: Boolean
        get() = maxPrice.isNotBlank() && maxPriceValue == null

    val visibleCars: List<CarListItem>
        get() {
            val matching = CarSearch.find(cars, query, userLocation)
            return if (sortByDistance && userLocation != null) CarSearch.sortByDistance(matching) else matching
        }
}

class CarListViewModel(
    private val repository: CarRepository,
    private val locationProvider: LocationProvider,
    session: Session,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CarListUiState(canAddCars = session.user?.role == Role.OWNER))
    val uiState: StateFlow<CarListUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    /** Loads the cars again with the current category and max price. A newer call replaces a running one. */
    fun refresh() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, error = null) }
        loadJob = viewModelScope.launch {
            val state = _uiState.value
            repository.getCars(state.category, state.maxPriceValue)
                .onSuccess { cars -> _uiState.update { it.copy(cars = cars, isLoading = false) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.toUserMessage()) } }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun onCategoryChange(category: CarCategory?) {
        _uiState.update { it.copy(category = category) }
        refresh()
    }

    fun onMaxPriceChange(maxPrice: String) {
        _uiState.update { it.copy(maxPrice = maxPrice) }
        if (!_uiState.value.isMaxPriceInvalid) refresh()
    }

    fun onSortByDistanceChange(sortByDistance: Boolean) {
        _uiState.update { it.copy(sortByDistance = sortByDistance) }
    }

    /** Call with the answer of the permission dialog, or with true when the permission was already granted. */
    fun onLocationPermissionResult(granted: Boolean) {
        if (!granted) {
            _uiState.update {
                it.copy(locationStatus = LocationStatus.DENIED, userLocation = null, sortByDistance = false)
            }
            return
        }
        _uiState.update { it.copy(locationStatus = LocationStatus.LOADING) }
        viewModelScope.launch {
            val location = locationProvider.currentLocation()
            _uiState.update {
                it.copy(
                    userLocation = location,
                    locationStatus = if (location != null) LocationStatus.AVAILABLE else LocationStatus.UNAVAILABLE,
                )
            }
        }
    }
}

/** Accepts a comma as decimal separator, because that is what Dutch keyboards give. */
internal fun String.toDecimalOrNull(): Double? = trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
