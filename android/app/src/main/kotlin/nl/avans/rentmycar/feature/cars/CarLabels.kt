package nl.avans.rentmycar.feature.cars

import androidx.annotation.StringRes
import nl.avans.rentmycar.R

@get:StringRes
val CarCategory.labelRes: Int
    get() = when (this) {
        CarCategory.ICE -> R.string.category_ice
        CarCategory.BEV -> R.string.category_bev
        CarCategory.FCEV -> R.string.category_fcev
    }

@get:StringRes
val FuelType.labelRes: Int
    get() = when (this) {
        FuelType.PETROL -> R.string.fuel_petrol
        FuelType.DIESEL -> R.string.fuel_diesel
        FuelType.LPG -> R.string.fuel_lpg
    }
