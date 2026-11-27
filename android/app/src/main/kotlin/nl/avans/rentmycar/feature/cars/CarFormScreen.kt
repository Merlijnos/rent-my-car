package nl.avans.rentmycar.feature.cars

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.avans.rentmycar.R
import nl.avans.rentmycar.core.common.asString
import nl.avans.rentmycar.feature.cars.location.hasLocationPermission
import nl.avans.rentmycar.feature.cars.location.rememberLocationPermissionRequest
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun CarFormScreen(
    carId: Int?,
    onSaved: () -> Unit,
    viewModel: CarFormViewModel = koinViewModel(parameters = { parametersOf(carId) }),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val requestPermission = rememberLocationPermissionRequest(viewModel::onLocationPermissionResult)

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onSaved()
    }

    CarFormContent(
        uiState = uiState,
        onCategoryChange = viewModel::onCategoryChange,
        onFuelTypeChange = viewModel::onFuelTypeChange,
        onBrandChange = viewModel::onBrandChange,
        onModelChange = viewModel::onModelChange,
        onPricePerDaypartChange = viewModel::onPricePerDaypartChange,
        onFixedCostsPerYearChange = viewModel::onFixedCostsPerYearChange,
        onConsumptionPer100KmChange = viewModel::onConsumptionPer100KmChange,
        onAddressChange = viewModel::onAddressChange,
        onFindAddress = viewModel::onFindAddress,
        onUseMyLocation = {
            if (context.hasLocationPermission()) viewModel.onLocationPermissionResult(true) else requestPermission()
        },
        onSave = viewModel::save,
    )
}

@Composable
fun CarFormContent(
    uiState: CarFormUiState,
    onCategoryChange: (CarCategory) -> Unit,
    onFuelTypeChange: (FuelType) -> Unit,
    onBrandChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onPricePerDaypartChange: (String) -> Unit,
    onFixedCostsPerYearChange: (String) -> Unit,
    onConsumptionPer100KmChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onFindAddress: () -> Unit,
    onUseMyLocation: () -> Unit,
    onSave: () -> Unit,
) {
    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    uiState.loadError?.let {
        Text(it.asString(), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
        return
    }

    val errors = uiState.errors
    val consumptionLabel = when (uiState.category) {
        CarCategory.ICE -> R.string.form_consumption_ice
        CarCategory.BEV -> R.string.form_consumption_bev
        CarCategory.FCEV -> R.string.form_consumption_fcev
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.form_category), style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CarCategory.entries.forEach { category ->
                FilterChip(
                    selected = uiState.category == category,
                    onClick = { onCategoryChange(category) },
                    label = { Text(category.name) },
                )
            }
        }
        if (uiState.category == CarCategory.ICE) {
            Text(stringResource(R.string.form_fuel_type), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FuelType.entries.forEach { fuelType ->
                    FilterChip(
                        selected = uiState.fuelType == fuelType,
                        onClick = { onFuelTypeChange(fuelType) },
                        label = { Text(stringResource(fuelType.labelRes)) },
                    )
                }
            }
        }
        FormField(uiState.brand, onBrandChange, R.string.form_brand, errors.brand)
        FormField(uiState.model, onModelChange, R.string.form_model, errors.model)
        FormField(
            uiState.pricePerDaypart, onPricePerDaypartChange, R.string.form_price, errors.pricePerDaypart,
            KeyboardType.Decimal,
        )
        FormField(
            uiState.fixedCostsPerYear, onFixedCostsPerYearChange, R.string.form_fixed_costs, errors.fixedCostsPerYear,
            KeyboardType.Decimal,
        )
        FormField(
            uiState.consumptionPer100Km, onConsumptionPer100KmChange, consumptionLabel, errors.consumptionPer100Km,
            KeyboardType.Decimal,
        )
        Text(stringResource(R.string.form_location_hint), style = MaterialTheme.typography.bodyMedium)
        FormField(uiState.address, onAddressChange, R.string.form_address, null)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onFindAddress, enabled = !uiState.isLocating) {
                Text(stringResource(R.string.form_address_find))
            }
            OutlinedButton(onClick = onUseMyLocation, enabled = !uiState.isLocating) {
                Text(stringResource(R.string.form_use_location))
            }
        }
        if (uiState.location != null) {
            Text(
                if (uiState.placeName != null) {
                    stringResource(R.string.form_location_found, uiState.placeName)
                } else {
                    stringResource(R.string.form_location_set)
                },
            )
        }
        uiState.locationError?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
        uiState.saveError?.let { Text(it.asString(), color = MaterialTheme.colorScheme.error) }
        Button(onClick = onSave, enabled = !uiState.isSaving, modifier = Modifier.fillMaxWidth()) {
            if (uiState.isSaving) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.form_save))
            }
        }
    }
}

@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    labelRes: Int,
    errorRes: Int?,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(labelRes)) },
        isError = errorRes != null,
        supportingText = if (errorRes != null) {
            { Text(stringResource(errorRes)) }
        } else {
            null
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
    )
}
