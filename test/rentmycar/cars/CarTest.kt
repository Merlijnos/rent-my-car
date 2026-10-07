package rentmycar.cars

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class CarTest {
    @Test
    fun `ice car cost per km uses the price per liter of its fuel`() {
        val petrol = IceCar("Volkswagen", "Polo", 15.0, 2_400.0, 6.0, FuelType.PETROL)
        val lpg = IceCar("Dacia", "Sandero", 12.5, 2_200.0, 6.0, FuelType.LPG)

        assertEquals(1.95 * 6.0 / 100, petrol.costPerKm(), 0.0001)
        assertEquals(0.85 * 6.0 / 100, lpg.costPerKm(), 0.0001)
    }

    @Test
    fun `bev cost per km uses the price per kWh`() {
        val car = BevCar("Kia", "Niro EV", 27.5, 3_400.0, 16.0)

        assertEquals(0.35 * 16.0 / 100, car.costPerKm(), 0.0001)
    }

    @Test
    fun `fcev cost per km uses the price per kg of hydrogen`() {
        val car = FcevCar("Toyota", "Mirai", 40.0, 5_200.0, 0.8)

        assertEquals(15.95 * 0.8 / 100, car.costPerKm(), 0.0001)
    }

    @Test
    fun `total cost of ownership adds the energy costs to the fixed costs`() {
        val car = BevCar("Kia", "Niro EV", 27.5, 3_000.0, 16.0)

        assertEquals(3_000.0 + 0.056 * 15_000, car.totalCostOfOwnership(15_000), 0.0001)
    }

    @Test
    fun `zero or negative consumption is rejected`() {
        assertFailsWith<IllegalArgumentException> { BevCar("Kia", "Niro EV", 27.5, 3_400.0, 0.0) }
        assertFailsWith<IllegalArgumentException> { FcevCar("Toyota", "Mirai", 40.0, 5_200.0, -1.0) }
    }

    @Test
    fun `negative prices are rejected`() {
        assertFailsWith<IllegalArgumentException> { BevCar("Kia", "Niro EV", -1.0, 3_400.0, 16.0) }
        assertFailsWith<IllegalArgumentException> { BevCar("Kia", "Niro EV", 27.5, -1.0, 16.0) }
    }

    @Test
    fun `create builds the subclass that matches the category`() {
        assertIs<IceCar>(Car.create(CarCategory.ICE, "Skoda", "Octavia", 22.5, 3_600.0, 4.6, FuelType.DIESEL))
        assertIs<BevCar>(Car.create(CarCategory.BEV, "Tesla", "Model 3", 35.0, 4_000.0, 14.5, null))
        assertIs<FcevCar>(Car.create(CarCategory.FCEV, "Hyundai", "Nexo", 37.5, 4_800.0, 0.95, null))
    }

    @Test
    fun `an ice car needs a fuel type`() {
        assertFailsWith<IllegalArgumentException> {
            Car.create(CarCategory.ICE, "Skoda", "Octavia", 22.5, 3_600.0, 4.6, fuelType = null)
        }
    }
}
