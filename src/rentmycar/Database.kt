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
import rentmycar.users.Role
import rentmycar.users.User
import rentmycar.users.UserRepository
import rentmycar.users.UserTable

/** Connects to [url], creates the tables and adds the example cars. */
fun connectDatabase(url: String): Database {
    val database = Database.connect(url, driver = "org.h2.Driver")
    transaction(database) {
        SchemaUtils.create(UserTable, CarTable)
    }
    val users = UserRepository(database)
    val seededUsers = seedUsers(users)
    seedCars(ExposedCarRepository(database), seededUsers)
    return database
}

private fun seedUsers(users: UserRepository): List<User> {
    val seed =
        listOf(
            User("paul", "paul123", "paul@rentmycar.nl", Role.OWNER),
            User("sara", "sara123", "sara@rentmycar.nl", Role.OWNER),
            User("lisa", "lisa123", "lisa@rentmycar.nl", Role.RENTER),
            User("tom", "tom123", "tom@rentmycar.nl", Role.RENTER),
        )
    return seed.map { users.create(it) }
}

private fun seedCars(cars: CarRepository, users: List<User>) {
    val paul = users.first { it.username == "paul" }.id
    val sara = users.first { it.username == "sara" }.id

    val seed =
        listOf(
            IceCar(
                brand = "Volkswagen",
                model = "Polo 1.0 TSI",
                pricePerDaypart = 15.0,
                fixedCostsPerYear = 2_400.0,
                consumptionPer100Km = 5.4,
                fuelType = FuelType.PETROL,
                ownerId = paul,
            ),
            IceCar(
                brand = "Skoda",
                model = "Octavia Combi 2.0 TDI",
                pricePerDaypart = 22.5,
                fixedCostsPerYear = 3_600.0,
                consumptionPer100Km = 4.6,
                fuelType = FuelType.DIESEL,
                ownerId = paul,
            ),
            IceCar(
                brand = "Dacia",
                model = "Sandero Stepway ECO-G",
                pricePerDaypart = 12.5,
                fixedCostsPerYear = 2_200.0,
                consumptionPer100Km = 7.2,
                fuelType = FuelType.LPG,
                ownerId = sara,
            ),
            BevCar(
                brand = "Kia",
                model = "Niro EV",
                pricePerDaypart = 27.5,
                fixedCostsPerYear = 3_400.0,
                consumptionPer100Km = 16.2,
                ownerId = sara,
            ),
            BevCar(
                brand = "Tesla",
                model = "Model 3",
                pricePerDaypart = 35.0,
                fixedCostsPerYear = 4_000.0,
                consumptionPer100Km = 14.5,
                ownerId = paul,
            ),
            BevCar(
                brand = "Volkswagen",
                model = "ID.3 Pro",
                pricePerDaypart = 25.0,
                fixedCostsPerYear = 3_200.0,
                consumptionPer100Km = 15.5,
                ownerId = sara,
            ),
            FcevCar(
                brand = "Toyota",
                model = "Mirai",
                pricePerDaypart = 40.0,
                fixedCostsPerYear = 5_200.0,
                consumptionPer100Km = 0.79,
                ownerId = paul,
            ),
            FcevCar(
                brand = "Hyundai",
                model = "Nexo",
                pricePerDaypart = 37.5,
                fixedCostsPerYear = 4_800.0,
                consumptionPer100Km = 0.95,
                ownerId = sara,
            ),
        )
    seed.forEach { cars.create(it) }
}
