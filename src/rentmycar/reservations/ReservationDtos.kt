package rentmycar.reservations

import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlinx.serialization.Serializable

@Serializable
data class ReservationRequest(
    val carId: Int,
    val startDate: String,
    val endDate: String,
) {
    fun toReservation(renterId: Int, pricePerDaypart: Double): Reservation =
        Reservation.book(
            carId,
            renterId,
            parseDate(startDate, "startDate"),
            parseDate(endDate, "endDate"),
            pricePerDaypart,
        )
}

@Serializable
data class ReservationResponse(
    val id: Int,
    val carId: Int,
    val renterId: Int,
    val startDate: String,
    val endDate: String,
    val dayparts: Int,
    val totalPrice: Double,
    val status: ReservationStatus,
)

fun Reservation.toResponse() =
    ReservationResponse(
        id = id,
        carId = carId,
        renterId = renterId,
        startDate = startDate.toString(),
        endDate = endDate.toString(),
        dayparts = dayparts(),
        totalPrice = totalPrice,
        status = status,
    )

private fun parseDate(value: String, field: String): LocalDate =
    try {
        LocalDate.parse(value)
    } catch (_: DateTimeParseException) {
        throw IllegalArgumentException("$field must be a date in the form yyyy-MM-dd")
    }
