package rentmycar

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import rentmycar.cars.BevCar
import rentmycar.cars.CarRepository
import rentmycar.cars.CarTable
import rentmycar.cars.ExposedCarRepository
import rentmycar.cars.FcevCar
import rentmycar.cars.FuelType
import rentmycar.cars.IceCar

/** Connects to [url], creates the tables and adds the example cars. */
fun connectDatabase(url: String): Database {
    val database = Database.connect(url, driver = "org.h2.Driver")
    transaction(database) {
        SchemaUtils.create(CarTable)
    }
    seedCars(ExposedCarRepository(database))
    return database
}

private fun seedCars(cars: CarRepository) {
    val seed =
        listOf(
            IceCar(
                brand = "Volkswagen",
                model = "Polo 1.0 TSI",
                pricePerDaypart = 15.0,
                fixedCostsPerYear = 2_400.0,
                consumptionPer100Km = 5.4,
                fuelType = FuelType.PETROL,
            ),
            IceCar(
                brand = "Skoda",
                model = "Octavia Combi 2.0 TDI",
                pricePerDaypart = 22.5,
                fixedCostsPerYear = 3_600.0,
                consumptionPer100Km = 4.6,
                fuelType = FuelType.DIESEL,
            ),
            IceCar(
                brand = "Dacia",
                model = "Sandero Stepway ECO-G",
                pricePerDaypart = 12.5,
                fixedCostsPerYear = 2_200.0,
                consumptionPer100Km = 7.2,
                fuelType = FuelType.LPG,
            ),
            BevCar(
                brand = "Kia",
                model = "Niro EV",
                pricePerDaypart = 27.5,
                fixedCostsPerYear = 3_400.0,
                consumptionPer100Km = 16.2,
            ),
            BevCar(
                brand = "Tesla",
                model = "Model 3",
                pricePerDaypart = 35.0,
                fixedCostsPerYear = 4_000.0,
                consumptionPer100Km = 14.5,
            ),
            BevCar(
                brand = "Volkswagen",
                model = "ID.3 Pro",
                pricePerDaypart = 25.0,
                fixedCostsPerYear = 3_200.0,
                consumptionPer100Km = 15.5,
            ),
            FcevCar(
                brand = "Toyota",
                model = "Mirai",
                pricePerDaypart = 40.0,
                fixedCostsPerYear = 5_200.0,
                consumptionPer100Km = 0.79,
            ),
            FcevCar(
                brand = "Hyundai",
                model = "Nexo",
                pricePerDaypart = 37.5,
                fixedCostsPerYear = 4_800.0,
                consumptionPer100Km = 0.95,
            ),
        )
    seed.forEach { cars.create(it) }
}
