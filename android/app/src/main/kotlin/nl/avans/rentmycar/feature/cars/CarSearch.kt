package nl.avans.rentmycar.feature.cars

import nl.avans.rentmycar.feature.cars.location.DistanceCalculator
import nl.avans.rentmycar.feature.cars.location.GeoPoint

/** Search and sort rules of the car list, kept out of the ViewModel so they can be tested on their own. */
object CarSearch {
    /** Keeps the cars where every word of [query] is in the brand or model, with the distance from [userLocation]. */
    fun find(cars: List<Car>, query: String, userLocation: GeoPoint?): List<CarListItem> =
        cars
            .filter { it.matches(query) }
            .map { car ->
                val distance = userLocation?.let { here -> car.location?.let { DistanceCalculator.distanceKm(here, it) } }
                CarListItem(car, distance)
            }

    /** Nearest first. Cars without a location go last. */
    fun sortByDistance(items: List<CarListItem>): List<CarListItem> =
        items.sortedBy { it.distanceKm ?: Double.MAX_VALUE }

    private fun Car.matches(query: String): Boolean {
        val words = query.trim().split(" ").filter { it.isNotEmpty() }
        return words.all { word ->
            brand.contains(word, ignoreCase = true) || model.contains(word, ignoreCase = true)
        }
    }
}
