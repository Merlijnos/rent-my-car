package nl.avans.rentmycar.core.navigation

import androidx.annotation.StringRes
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import nl.avans.rentmycar.R

@Serializable
data object Login : NavKey

@Serializable
data object CarList : NavKey

@Serializable
data class CarDetail(val carId: Int) : NavKey

/** [carId] is null when a new car is created. */
@Serializable
data class CarForm(val carId: Int? = null) : NavKey

@StringRes
fun NavKey?.titleRes(): Int =
    when (this) {
        is CarList -> R.string.cars_title
        is CarDetail -> R.string.car_detail_title
        is CarForm -> if (carId == null) R.string.car_form_create_title else R.string.car_form_edit_title
        else -> R.string.app_name
    }
