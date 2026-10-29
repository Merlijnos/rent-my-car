package rentmycar.reservations

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReservationTest {
    private val start = LocalDate.parse("2026-07-06")
    private val end = LocalDate.parse("2026-07-08")

    @Test
    fun `price is two dayparts per day times the price per daypart`() {
        val threeDays = Reservation.book(1, 3, start, end, 15.0)
        assertEquals(3, threeDays.numberOfDays())
        assertEquals(6, threeDays.dayparts())
        assertEquals(90.0, threeDays.totalPrice)

        val oneDay = Reservation.book(1, 3, start, start, 27.5)
        assertEquals(2, oneDay.dayparts())
        assertEquals(55.0, oneDay.totalPrice)
    }

    @Test
    fun `total price is rounded to cents`() {
        val reservation = Reservation.book(1, 3, start, start, 1.0 / 3)
        assertEquals(0.67, reservation.totalPrice, 0.001)
    }

    @Test
    fun `an active reservation blocks overlapping days only`() {
        val pending = Reservation.book(1, 3, start, end, 15.0)

        assertTrue(pending.blocks(LocalDate.parse("2026-07-08"), LocalDate.parse("2026-07-09")))
        assertFalse(pending.blocks(LocalDate.parse("2026-07-09"), LocalDate.parse("2026-07-10")))
        assertTrue(pending.confirm().blocks(start, start))
        assertFalse(pending.cancel().blocks(start, end))
        assertFalse(pending.reject().blocks(start, end))
    }

    @Test
    fun `confirm reject and cancel follow the status`() {
        val pending = Reservation.book(1, 3, start, end, 15.0)

        assertEquals(ReservationStatus.CONFIRMED, pending.confirm().status)
        assertEquals(ReservationStatus.REJECTED, pending.reject().status)
        assertEquals(ReservationStatus.CANCELED, pending.cancel().status)
        assertEquals(ReservationStatus.CANCELED, pending.confirm().cancel().status)

        val confirmed = pending.confirm()
        assertEquals(
            "Only a pending reservation can be confirmed",
            assertFailsWith<IllegalArgumentException> { confirmed.confirm() }.message,
        )
        assertEquals(
            "Only a pending or confirmed reservation can be canceled",
            assertFailsWith<IllegalArgumentException> { pending.reject().cancel() }.message,
        )
    }

    @Test
    fun `end before start or a period over a year is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            Reservation.book(1, 3, end, start, 15.0)
        }
        assertFailsWith<IllegalArgumentException> {
            Reservation.book(1, 3, LocalDate.parse("2026-01-01"), LocalDate.parse("2027-01-02"), 15.0)
        }
        val fullYear = Reservation.book(1, 3, LocalDate.parse("2026-01-01"), LocalDate.parse("2027-01-01"), 10.0)
        assertEquals(366, fullYear.numberOfDays())
    }

    @Test
    fun `a negative price per daypart is rejected`() {
        assertFailsWith<IllegalArgumentException> { Reservation.book(1, 3, start, end, -1.0) }
    }
}
