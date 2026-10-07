package rentmycar.cars

import org.jetbrains.exposed.dao.id.IntIdTable

object CarTable : IntIdTable("cars") {
    val category = enumerationByName<CarCategory>("category", 10)
    val brand = varchar("brand", 50)
    val model = varchar("model", 50)
    val pricePerDaypart = double("price_per_daypart")
    val fixedCostsPerYear = double("fixed_costs_per_year")
    val consumptionPer100Km = double("consumption_per_100_km")
    val fuelType = enumerationByName<FuelType>("fuel_type", 10).nullable()
}
