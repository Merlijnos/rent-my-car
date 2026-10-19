package rentmycar.cars

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

/** Stores cars. The routes only use this interface, so they do not depend on the database. */
interface CarRepository {
    fun findAll(category: CarCategory? = null, maxPricePerDaypart: Double? = null): List<Car>

    fun findById(id: Int): Car?

    /** Returns the stored car with its new id. */
    fun create(car: Car): Car

    /** Returns false when no car with this id exists. */
    fun update(car: Car): Boolean

    /** Returns false when no car with this id exists. */
    fun delete(id: Int): Boolean
}

/** Stores cars in [CarTable] with Exposed. */
class ExposedCarRepository(private val database: Database) : CarRepository {
    override fun findAll(category: CarCategory?, maxPricePerDaypart: Double?): List<Car> =
        transaction(database) {
            val query = CarTable.selectAll().orderBy(CarTable.id)
            if (category != null) query.andWhere { CarTable.category eq category }
            if (maxPricePerDaypart != null) query.andWhere { CarTable.pricePerDaypart lessEq maxPricePerDaypart }
            query.map { it.toCar() }
        }

    override fun findById(id: Int): Car? =
        transaction(database) {
            CarTable.selectAll().where { CarTable.id eq id }.singleOrNull()?.toCar()
        }

    override fun create(car: Car): Car =
        transaction(database) {
            val id = CarTable.insertAndGetId { it.setValues(car) }
            CarTable.selectAll().where { CarTable.id eq id }.single().toCar()
        }

    override fun update(car: Car): Boolean =
        transaction(database) {
            CarTable.update({ CarTable.id eq car.id }) { it.setValues(car) } > 0
        }

    override fun delete(id: Int): Boolean =
        transaction(database) {
            CarTable.deleteWhere { CarTable.id eq id } > 0
        }

    private fun ResultRow.toCar(): Car =
        Car.create(
            category = this[CarTable.category],
            brand = this[CarTable.brand],
            model = this[CarTable.model],
            pricePerDaypart = this[CarTable.pricePerDaypart],
            fixedCostsPerYear = this[CarTable.fixedCostsPerYear],
            consumptionPer100Km = this[CarTable.consumptionPer100Km],
            fuelType = this[CarTable.fuelType],
            id = this[CarTable.id].value,
        )

    private fun UpdateBuilder<*>.setValues(car: Car) {
        this[CarTable.category] = car.category
        this[CarTable.brand] = car.brand
        this[CarTable.model] = car.model
        this[CarTable.pricePerDaypart] = car.pricePerDaypart
        this[CarTable.fixedCostsPerYear] = car.fixedCostsPerYear
        this[CarTable.consumptionPer100Km] = car.consumptionPer100Km
        this[CarTable.fuelType] = (car as? IceCar)?.fuelType
    }
}
