package rentmycar.reservations

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.round

/** Where a booking is in its life: requested, accepted, refused or called off. */
enum class ReservationStatus {
    PENDING,
    CONFIRMED,
    REJECTED,
    CANCELED,
}

/**
 * A renter books one car from [startDate] through [endDate], both inclusive.
 * Each day counts as [DAYPARTS_PER_DAY] dayparts (morning and afternoon),
 * and [totalPrice] is those dayparts times the car's price per daypart at booking time.
 * [id] is 0 until the reservation is stored.
 */
data class Reservation(
    val carId: Int,
    val renterId: Int,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val totalPrice: Double,
    val status: ReservationStatus = ReservationStatus.PENDING,
    val id: Int = 0,
) {
    init {
        require(!endDate.isBefore(startDate)) { "endDate must not be before startDate" }
        val days = ChronoUnit.DAYS.between(startDate, endDate) + 1
        require(days <= MAX_DAYS) { "a reservation can cover at most $MAX_DAYS days" }
        require(totalPrice >= 0) { "totalPrice must not be negative" }
    }

    fun numberOfDays(): Int = (ChronoUnit.DAYS.between(startDate, endDate) + 1).toInt()

    fun dayparts(): Int = numberOfDays() * DAYPARTS_PER_DAY

    /** Pending and confirmed bookings keep the car unavailable. */
    fun isActive(): Boolean = status == ReservationStatus.PENDING || status == ReservationStatus.CONFIRMED

    /** True when this active booking shares a day with [start]..[end] (inclusive). */
    fun blocks(start: LocalDate, end: LocalDate): Boolean =
        isActive() && !endDate.isBefore(start) && !end.isBefore(startDate)

    fun confirm(): Reservation {
        require(status == ReservationStatus.PENDING) { "Only a pending reservation can be confirmed" }
        return copy(status = ReservationStatus.CONFIRMED)
    }

    fun reject(): Reservation {
        require(status == ReservationStatus.PENDING) { "Only a pending reservation can be rejected" }
        return copy(status = ReservationStatus.REJECTED)
    }

    fun cancel(): Reservation {
        require(status == ReservationStatus.PENDING || status == ReservationStatus.CONFIRMED) {
            "Only a pending or confirmed reservation can be canceled"
        }
        return copy(status = ReservationStatus.CANCELED)
    }

    companion object {
        const val DAYPARTS_PER_DAY = 2
        const val MAX_DAYS = 366

        fun book(
            carId: Int,
            renterId: Int,
            startDate: LocalDate,
            endDate: LocalDate,
            pricePerDaypart: Double,
        ): Reservation {
            require(pricePerDaypart >= 0) { "pricePerDaypart must not be negative" }
            val draft = Reservation(carId, renterId, startDate, endDate, totalPrice = 0.0)
            val total = round(draft.dayparts() * pricePerDaypart * 100) / 100
            return draft.copy(totalPrice = total)
        }
    }
}
