package nl.avans.rentmycar.feature.cars

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import nl.avans.rentmycar.core.common.UserMessage
import nl.avans.rentmycar.core.common.toUserMessage
import nl.avans.rentmycar.core.session.Session
import nl.avans.rentmycar.feature.cars.location.DistanceCalculator
import nl.avans.rentmycar.feature.cars.location.GeoPoint
import nl.avans.rentmycar.feature.cars.location.LocationProvider

const val DEFAULT_KM_PER_YEAR = "15000"

data class CarDetailUiState(
    val car: Car? = null,
    val isLoading: Boolean = true,
    val error: UserMessage? = null,
    val currentUserId: Int? = null,
    val userLocation: GeoPoint? = null,
    val kmPerYear: String = DEFAULT_KM_PER_YEAR,
    val isKmPerYearInvalid: Boolean = false,
    val costs: CarCosts? = null,
    val costsError: UserMessage? = null,
    val showDeleteDialog: Boolean = false,
    val isDeleting: Boolean = false,
    val deleteError: UserMessage? = null,
    val isDeleted: Boolean = false,
) {
    /** Only the owner of the car may edit or delete it. */
    val isOwner: Boolean
        get() = car != null && car.ownerId == currentUserId

    val distanceKm: Double?
        get() {
            val from = userLocation ?: return null
            val to = car?.location ?: return null
            return DistanceCalculator.distanceKm(from, to)
        }
}

class CarDetailViewModel(
    private val carId: Int,
    private val repository: CarRepository,
    private val locationProvider: LocationProvider,
    session: Session,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CarDetailUiState(currentUserId = session.user?.id))
    val uiState: StateFlow<CarDetailUiState> = _uiState.asStateFlow()

    /** Loads the car and its costs. The car on screen stays visible while it reloads. */
    fun load() {
        _uiState.update { it.copy(isLoading = it.car == null, error = null) }
        viewModelScope.launch {
            repository.getCar(carId)
                .onSuccess { car ->
                    _uiState.update { it.copy(car = car, isLoading = false) }
                    calculateCosts()
                }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.toUserMessage()) } }
        }
    }

    fun onKmPerYearChange(kmPerYear: String) {
        _uiState.update { it.copy(kmPerYear = kmPerYear, isKmPerYearInvalid = false) }
    }

    fun calculateCosts() {
        val kmPerYear = _uiState.value.kmPerYear.trim().toIntOrNull()
        if (kmPerYear == null || kmPerYear < 0) {
            _uiState.update { it.copy(isKmPerYearInvalid = true) }
            return
        }
        viewModelScope.launch {
            repository.getCosts(carId, kmPerYear)
                .onSuccess { costs -> _uiState.update { it.copy(costs = costs, costsError = null) } }
                .onFailure { e -> _uiState.update { it.copy(costsError = e.toUserMessage()) } }
        }
    }

    /** Call with true when the location permission is granted. Without it the distance stays hidden. */
    fun onLocationPermissionResult(granted: Boolean) {
        if (!granted) return
        viewModelScope.launch {
            val location = locationProvider.currentLocation()
            _uiState.update { it.copy(userLocation = location) }
        }
    }

    fun onDeleteClick() {
        _uiState.update { it.copy(showDeleteDialog = true, deleteError = null) }
    }

    fun onDeleteDismiss() {
        _uiState.update { it.copy(showDeleteDialog = false) }
    }

    fun onDeleteConfirm() {
        if (!_uiState.value.isOwner) return
        _uiState.update { it.copy(showDeleteDialog = false, isDeleting = true) }
        viewModelScope.launch {
            repository.deleteCar(carId)
                .onSuccess { _uiState.update { it.copy(isDeleting = false, isDeleted = true) } }
                .onFailure { e -> _uiState.update { it.copy(isDeleting = false, deleteError = e.toUserMessage()) } }
        }
    }
}
