package nl.avans.rentmycar.feature.cars

import nl.avans.rentmycar.R

/** The form rules, matching what the backend accepts. Pure, so every rule can be unit tested. */
object CarFormValidator {
    private const val MAX_NAME_LENGTH = 50

    fun validate(form: CarFormUiState): CarFormValidation {
        val price = form.pricePerDaypart.toDecimalOrNull()
        val fixedCosts = form.fixedCostsPerYear.toDecimalOrNull()
        val consumption = form.consumptionPer100Km.toDecimalOrNull()

        val errors = CarFormErrors(
            brand = R.string.form_error_brand.takeIf { form.brand.isBlank() || form.brand.trim().length > MAX_NAME_LENGTH },
            model = R.string.form_error_model.takeIf { form.model.isBlank() || form.model.trim().length > MAX_NAME_LENGTH },
            pricePerDaypart = R.string.form_error_amount.takeIf { price == null || price < 0 },
            fixedCostsPerYear = R.string.form_error_amount.takeIf { fixedCosts == null || fixedCosts < 0 },
            consumptionPer100Km = R.string.form_error_consumption.takeIf { consumption == null || consumption <= 0 },
        )
        if (errors.hasErrors || price == null || fixedCosts == null || consumption == null) {
            return CarFormValidation(errors, null)
        }

        val request = CarRequest(
            category = form.category,
            brand = form.brand.trim(),
            model = form.model.trim(),
            pricePerDaypart = price,
            fixedCostsPerYear = fixedCosts,
            consumptionPer100Km = consumption,
            fuelType = form.fuelType.takeIf { form.category == CarCategory.ICE },
            latitude = form.location?.latitude,
            longitude = form.location?.longitude,
        )
        return CarFormValidation(errors, request)
    }
}
