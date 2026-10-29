package rentmycar.reservations

import java.time.LocalDate
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

/** Stores reservations. The routes only use this interface, so they do not depend on the database. */
interface ReservationRepository {
    fun findAll(
        renterId: Int? = null,
        carId: Int? = null,
        carIds: Collection<Int>? = null,
        status: ReservationStatus? = null,
    ): List<Reservation>

    fun findById(id: Int): Reservation?

    /** An active reservation for this car that shares a day with the given range, if there is one. */
    fun findActiveOverlap(carId: Int, startDate: LocalDate, endDate: LocalDate): Reservation?

    /** True when the car still has a pending or confirmed reservation. */
    fun hasActiveForCar(carId: Int): Boolean

    /** Returns the stored reservation with its new id. */
    fun create(reservation: Reservation): Reservation

    /** Returns false when no reservation with this id exists. */
    fun update(reservation: Reservation): Boolean
}

/** Stores reservations in [ReservationTable] with Exposed. */
class ExposedReservationRepository(private val database: Database) : ReservationRepository {
    override fun findAll(
        renterId: Int?,
        carId: Int?,
        carIds: Collection<Int>?,
        status: ReservationStatus?,
    ): List<Reservation> =
        transaction(database) {
            if (carIds != null && carIds.isEmpty()) return@transaction emptyList()
            val query = ReservationTable.selectAll().orderBy(ReservationTable.id)
            if (renterId != null) query.andWhere { ReservationTable.renterId eq renterId }
            if (carId != null) query.andWhere { ReservationTable.carId eq carId }
            if (carIds != null) query.andWhere { ReservationTable.carId inList carIds }
            if (status != null) query.andWhere { ReservationTable.status eq status }
            query.map { it.toReservation() }
        }

    override fun findById(id: Int): Reservation? =
        transaction(database) {
            ReservationTable.selectAll().where { ReservationTable.id eq id }.singleOrNull()?.toReservation()
        }

    override fun findActiveOverlap(carId: Int, startDate: LocalDate, endDate: LocalDate): Reservation? =
        findAll(carId = carId).firstOrNull { it.blocks(startDate, endDate) }

    override fun hasActiveForCar(carId: Int): Boolean = findAll(carId = carId).any { it.isActive() }

    override fun create(reservation: Reservation): Reservation =
        transaction(database) {
            val id = ReservationTable.insertAndGetId { it.setValues(reservation) }
            ReservationTable.selectAll().where { ReservationTable.id eq id }.single().toReservation()
        }

    override fun update(reservation: Reservation): Boolean =
        transaction(database) {
            ReservationTable.update({ ReservationTable.id eq reservation.id }) { it.setValues(reservation) } > 0
        }

    private fun ResultRow.toReservation(): Reservation =
        Reservation(
            carId = this[ReservationTable.carId],
            renterId = this[ReservationTable.renterId],
            startDate = LocalDate.parse(this[ReservationTable.startDate]),
            endDate = LocalDate.parse(this[ReservationTable.endDate]),
            totalPrice = this[ReservationTable.totalPrice],
            status = this[ReservationTable.status],
            id = this[ReservationTable.id].value,
        )

    private fun UpdateBuilder<*>.setValues(reservation: Reservation) {
        this[ReservationTable.carId] = reservation.carId
        this[ReservationTable.renterId] = reservation.renterId
        this[ReservationTable.startDate] = reservation.startDate.toString()
        this[ReservationTable.endDate] = reservation.endDate.toString()
        this[ReservationTable.totalPrice] = reservation.totalPrice
        this[ReservationTable.status] = reservation.status
    }
}
