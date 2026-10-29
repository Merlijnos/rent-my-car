package rentmycar.reservations

import org.jetbrains.exposed.dao.id.IntIdTable

object ReservationTable : IntIdTable("reservations") {
    val carId = integer("car_id")
    val renterId = integer("renter_id")
    val startDate = varchar("start_date", 10)
    val endDate = varchar("end_date", 10)
    val totalPrice = double("total_price")
    val status = enumerationByName<ReservationStatus>("status", 10)
}
