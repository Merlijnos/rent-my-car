package nl.avans.rentmycar.feature.cars

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.avans.rentmycar.R
import nl.avans.rentmycar.core.common.asString
import nl.avans.rentmycar.feature.cars.location.hasLocationPermission
import nl.avans.rentmycar.feature.cars.location.openWalkingRoute
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun CarDetailScreen(
    carId: Int,
    onEdit: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: CarDetailViewModel = koinViewModel(parameters = { parametersOf(carId) }),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val routeUnavailable = stringResource(R.string.detail_route_unavailable)

    // Runs every time the screen comes back, so an edit is visible after saving.
    LaunchedEffect(Unit) {
        viewModel.load()
        viewModel.onLocationPermissionResult(context.hasLocationPermission())
    }
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onDeleted()
    }

    CarDetailContent(
        uiState = uiState,
        onKmPerYearChange = viewModel::onKmPerYearChange,
        onCalculate = viewModel::calculateCosts,
        onRoute = {
            val destination = uiState.car?.location
            if (destination != null && !context.openWalkingRoute(destination)) {
                Toast.makeText(context, routeUnavailable, Toast.LENGTH_LONG).show()
            }
        },
        onEdit = onEdit,
        onDeleteClick = viewModel::onDeleteClick,
        onDeleteConfirm = viewModel::onDeleteConfirm,
        onDeleteDismiss = viewModel::onDeleteDismiss,
        onRetry = viewModel::load,
    )
}

@Composable
fun CarDetailContent(
    uiState: CarDetailUiState,
    onKmPerYearChange: (String) -> Unit,
    onCalculate: () -> Unit,
    onRoute: () -> Unit,
    onEdit: () -> Unit,
    onDeleteClick: () -> Unit,
    onDeleteConfirm: () -> Unit,
    onDeleteDismiss: () -> Unit,
    onRetry: () -> Unit,
) {
    val car = uiState.car
    when {
        car != null -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("${car.brand} ${car.model}", style = MaterialTheme.typography.headlineMedium)
                CarFacts(car, uiState.distanceKm)
                if (car.location != null) {
                    Button(onClick = onRoute) { Text(stringResource(R.string.detail_route)) }
                }
                HorizontalDivider()
                CostsSection(uiState, onKmPerYearChange, onCalculate)
                if (uiState.isOwner) {
                    HorizontalDivider()
                    uiState.deleteError?.let { Text(it.asString(), color = MaterialTheme.colorScheme.error) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onEdit) { Text(stringResource(R.string.detail_edit)) }
                        OutlinedButton(onClick = onDeleteClick, enabled = !uiState.isDeleting) {
                            Text(stringResource(R.string.detail_delete))
                        }
                    }
                }
            }
            if (uiState.showDeleteDialog) {
                DeleteDialog("${car.brand} ${car.model}", onDeleteConfirm, onDeleteDismiss)
            }
        }
        uiState.error != null -> Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(uiState.error.asString(), color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = onRetry) { Text(stringResource(R.string.cars_retry)) }
        }
        else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    }
}

@Composable
private fun CarFacts(car: Car, distanceKm: Double?) {
    val consumptionRes = when (car.category) {
        CarCategory.ICE -> R.string.consumption_ice
        CarCategory.BEV -> R.string.consumption_bev
        CarCategory.FCEV -> R.string.consumption_fcev
    }
    val location = car.location
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FactRow(R.string.detail_category, stringResource(car.category.labelRes))
        car.fuelType?.let { FactRow(R.string.detail_fuel, stringResource(it.labelRes)) }
        FactRow(R.string.detail_price, stringResource(R.string.car_price_per_daypart, car.pricePerDaypart))
        FactRow(R.string.detail_fixed_costs, stringResource(R.string.detail_fixed_costs_value, car.fixedCostsPerYear))
        FactRow(R.string.detail_consumption, stringResource(consumptionRes, car.consumptionPer100Km))
        FactRow(
            R.string.detail_location,
            if (location != null) {
                stringResource(R.string.detail_location_set)
            } else {
                stringResource(R.string.detail_location_none)
            },
        )
        FactRow(
            R.string.detail_distance,
            if (distanceKm != null) {
                stringResource(R.string.car_distance_away, distanceKm)
            } else {
                stringResource(R.string.detail_distance_unknown)
            },
        )
    }
}

@Composable
private fun FactRow(labelRes: Int, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(labelRes), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value)
    }
}

@Composable
private fun CostsSection(
    uiState: CarDetailUiState,
    onKmPerYearChange: (String) -> Unit,
    onCalculate: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.detail_costs_title), style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = uiState.kmPerYear,
            onValueChange = onKmPerYearChange,
            label = { Text(stringResource(R.string.detail_km_per_year)) },
            isError = uiState.isKmPerYearInvalid,
            supportingText = if (uiState.isKmPerYearInvalid) {
                { Text(stringResource(R.string.detail_km_per_year_invalid)) }
            } else {
                null
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(onClick = onCalculate) { Text(stringResource(R.string.detail_calculate)) }
        uiState.costsError?.let { Text(it.asString(), color = MaterialTheme.colorScheme.error) }
        uiState.costs?.let { costs ->
            FactRow(R.string.detail_cost_per_km, stringResource(R.string.detail_cost_per_km_value, costs.costPerKm))
            FactRow(
                R.string.detail_total_cost,
                stringResource(R.string.detail_total_cost_value, costs.totalCostOfOwnership),
            )
        }
    }
}

@Composable
private fun DeleteDialog(carName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_dialog_title)) },
        text = { Text(stringResource(R.string.delete_dialog_text, carName)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.detail_delete)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
