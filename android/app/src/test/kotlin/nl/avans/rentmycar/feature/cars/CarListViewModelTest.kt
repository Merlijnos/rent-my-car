package nl.avans.rentmycar.feature.cars

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import nl.avans.rentmycar.core.session.Session
import nl.avans.rentmycar.feature.cars.location.GeoPoint
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CarListViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val breda = GeoPoint(51.5719, 4.7683)
    private val denBosch = GeoPoint(51.6978, 5.3037)
    private val far = testCar(1, "Volkswagen", "Polo", location = denBosch)
    private val near = testCar(2, "Tesla", "Model 3", CarCategory.BEV, location = breda)
    private val unknown = testCar(3, "Kia", "Niro EV", CarCategory.BEV)

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(phoneLocation: GeoPoint): CarListViewModel {
        val session = Session(FakeSessionStorage(), CoroutineScope(dispatcher))
        val repository = FakeCarRepository(listOf(far, near, unknown))
        return CarListViewModel(repository, FakeLocationProvider(phoneLocation), session).also { it.refresh() }
    }

    @Test
    fun `sort by distance puts the nearest car first and cars without location last`() {
        val viewModel = viewModel(phoneLocation = breda)

        viewModel.onLocationPermissionResult(true)
        viewModel.onSortByDistanceChange(true)

        val ids = viewModel.uiState.value.visibleCars.map { it.car.id }
        assertEquals(listOf(2, 1, 3), ids)
    }

    @Test
    fun `denied permission keeps the list without distances`() {
        val viewModel = viewModel(phoneLocation = breda)

        viewModel.onLocationPermissionResult(false)

        val state = viewModel.uiState.value
        assertEquals(LocationStatus.DENIED, state.locationStatus)
        assertTrue(state.visibleCars.all { it.distanceKm == null })
    }
}
