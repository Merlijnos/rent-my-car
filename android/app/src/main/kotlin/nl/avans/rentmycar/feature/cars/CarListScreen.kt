package nl.avans.rentmycar.feature.cars

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.avans.rentmycar.R
import nl.avans.rentmycar.core.common.asString
import nl.avans.rentmycar.feature.cars.location.hasLocationPermission
import nl.avans.rentmycar.feature.cars.location.rememberLocationPermissionRequest
import org.koin.androidx.compose.koinViewModel

@Composable
fun CarListScreen(
    onCarClick: (carId: Int) -> Unit,
    onAddCar: () -> Unit,
    viewModel: CarListViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val requestPermission = rememberLocationPermissionRequest(viewModel::onLocationPermissionResult)

    // Runs every time the screen comes back, so a car that was added, edited or deleted shows up.
    LaunchedEffect(Unit) { viewModel.refresh() }

    // Also covers coming back from the app settings after the user allowed location there.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val status = uiState.locationStatus
        val isBusyOrDone = status == LocationStatus.AVAILABLE || status == LocationStatus.LOADING
        if (!isBusyOrDone && context.hasLocationPermission()) viewModel.onLocationPermissionResult(true)
    }

    CarListContent(
        uiState = uiState,
        onQueryChange = viewModel::onQueryChange,
        onCategoryChange = viewModel::onCategoryChange,
        onMaxPriceChange = viewModel::onMaxPriceChange,
        onSortByDistanceChange = viewModel::onSortByDistanceChange,
        onRequestLocation = requestPermission,
        onOpenAppSettings = {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
            )
        },
        onRetry = viewModel::refresh,
        onCarClick = onCarClick,
        onAddCar = onAddCar,
    )
}

@Composable
fun CarListContent(
    uiState: CarListUiState,
    onQueryChange: (String) -> Unit,
    onCategoryChange: (CarCategory?) -> Unit,
    onMaxPriceChange: (String) -> Unit,
    onSortByDistanceChange: (Boolean) -> Unit,
    onRequestLocation: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onRetry: () -> Unit,
    onCarClick: (Int) -> Unit,
    onAddCar: () -> Unit,
) {
    val visibleCars = uiState.visibleCars
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Filters(uiState, onQueryChange, onCategoryChange, onMaxPriceChange, onSortByDistanceChange)
            }
            item {
                LocationBanner(uiState.locationStatus, onRequestLocation, onOpenAppSettings)
            }
            when {
                uiState.isLoading && uiState.cars.isEmpty() -> item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null -> item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(uiState.error.asString(), color = MaterialTheme.colorScheme.error)
                        OutlinedButton(onClick = onRetry) { Text(stringResource(R.string.cars_retry)) }
                    }
                }
                visibleCars.isEmpty() -> item {
                    val message = if (uiState.cars.isEmpty()) R.string.cars_empty else R.string.cars_no_match
                    Text(stringResource(message))
                }
                else -> items(visibleCars, key = { it.car.id }) { item ->
                    CarCard(item, onClick = { onCarClick(item.car.id) })
                }
            }
            // Space so the floating button never hides the last car.
            item { Box(Modifier.padding(bottom = 56.dp)) }
        }
        if (uiState.canAddCars) {
            FloatingActionButton(
                onClick = onAddCar,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cars_add))
            }
        }
    }
}

@Composable
private fun Filters(
    uiState: CarListUiState,
    onQueryChange: (String) -> Unit,
    onCategoryChange: (CarCategory?) -> Unit,
    onMaxPriceChange: (String) -> Unit,
    onSortByDistanceChange: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = uiState.query,
            onValueChange = onQueryChange,
            label = { Text(stringResource(R.string.cars_search_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CarCategory.entries.forEach { category ->
                FilterChip(
                    selected = uiState.category == category,
                    onClick = { onCategoryChange(if (uiState.category == category) null else category) },
                    label = { Text(category.name) },
                )
            }
        }
        OutlinedTextField(
            value = uiState.maxPrice,
            onValueChange = onMaxPriceChange,
            label = { Text(stringResource(R.string.cars_max_price_label)) },
            isError = uiState.isMaxPriceInvalid,
            supportingText = if (uiState.isMaxPriceInvalid) {
                { Text(stringResource(R.string.cars_max_price_invalid)) }
            } else {
                null
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        FilterChip(
            selected = uiState.sortByDistance,
            onClick = { onSortByDistanceChange(!uiState.sortByDistance) },
            enabled = uiState.userLocation != null,
            label = { Text(stringResource(R.string.cars_sort_distance)) },
        )
    }
}

@Composable
private fun LocationBanner(status: LocationStatus, onRequestLocation: () -> Unit, onOpenAppSettings: () -> Unit) {
    if (status == LocationStatus.AVAILABLE) return
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (status) {
                LocationStatus.NOT_ASKED -> {
                    Text(stringResource(R.string.location_rationale_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.location_rationale_text))
                    Button(onClick = onRequestLocation) { Text(stringResource(R.string.location_allow)) }
                }
                LocationStatus.LOADING -> Text(stringResource(R.string.location_loading))
                LocationStatus.DENIED -> {
                    Text(stringResource(R.string.location_denied))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onRequestLocation) { Text(stringResource(R.string.cars_retry)) }
                        OutlinedButton(onClick = onOpenAppSettings) { Text(stringResource(R.string.location_app_settings)) }
                    }
                }
                LocationStatus.UNAVAILABLE -> {
                    Text(stringResource(R.string.location_unavailable))
                    Button(onClick = onRequestLocation) { Text(stringResource(R.string.cars_retry)) }
                }
                LocationStatus.AVAILABLE -> Unit
            }
        }
    }
}

@Composable
private fun CarCard(item: CarListItem, onClick: () -> Unit) {
    val car = item.car
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${car.brand} ${car.model}", style = MaterialTheme.typography.titleMedium)
            Text(stringResource(car.category.labelRes), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.car_price_per_daypart, car.pricePerDaypart))
            item.distanceKm?.let {
                Text(stringResource(R.string.car_distance_away, it), color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
