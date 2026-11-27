package nl.avans.rentmycar.feature.cars

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import nl.avans.rentmycar.R
import nl.avans.rentmycar.core.FakeSessionStorage
import nl.avans.rentmycar.core.common.UserMessage
import nl.avans.rentmycar.core.network.ApiException
import nl.avans.rentmycar.core.session.AuthSession
import nl.avans.rentmycar.core.session.Role
import nl.avans.rentmycar.core.session.Session
import nl.avans.rentmycar.core.session.SessionUser
import nl.avans.rentmycar.feature.cars.location.GeoPoint
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class CarListViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    private val breda = GeoPoint(51.5719, 4.7683)
    private val denBosch = GeoPoint(51.6978, 5.3037)
    private val polo = testCar(1, "Volkswagen", "Polo", pricePerDaypart = 15.0, location = denBosch)
    private val tesla = testCar(2, "Tesla", "Model 3", CarCategory.BEV, 45.0, location = breda)
    private val niro = testCar(3, "Kia", "Niro EV", CarCategory.BEV, 30.0)

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun session(role: Role = Role.OWNER) =
        Session(FakeSessionStorage(AuthSession("jwt", SessionUser(1, "paul", role))), CoroutineScope(dispatcher))

    private fun viewModel(
        repository: FakeCarRepository = FakeCarRepository(listOf(polo, tesla, niro)),
        location: FakeLocationProvider = FakeLocationProvider(breda),
        session: Session = session(),
    ) = CarListViewModel(repository, location, session).also { it.refresh() }

    private fun CarListViewModel.visibleIds() = uiState.value.visibleCars.map { it.car.id }

    @Test
    fun refreshShowsAllCars() {
        val viewModel = viewModel()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(listOf(1, 2, 3), viewModel.visibleIds())
    }

    @Test
    fun searchMatchesBrandAndModelIgnoringCase() {
        val viewModel = viewModel()

        viewModel.onQueryChange("tesla")
        assertEquals(listOf(2), viewModel.visibleIds())

        viewModel.onQueryChange("NIRO")
        assertEquals(listOf(3), viewModel.visibleIds())

        viewModel.onQueryChange("kia niro")
        assertEquals(listOf(3), viewModel.visibleIds())

        viewModel.onQueryChange("zzz")
        assertTrue(viewModel.visibleIds().isEmpty())
    }

    @Test
    fun categoryAndMaxPriceAreSentToTheBackend() {
        val repository = FakeCarRepository(listOf(polo, tesla, niro))
        val viewModel = viewModel(repository)

        viewModel.onCategoryChange(CarCategory.BEV)
        assertEquals(CarCategory.BEV, repository.lastCategory)
        assertEquals(listOf(2, 3), viewModel.visibleIds())

        viewModel.onMaxPriceChange("30,50")
        assertEquals(30.5, repository.lastMaxPrice!!, 0.0)
        assertEquals(listOf(3), viewModel.visibleIds())

        viewModel.onCategoryChange(null)
        viewModel.onMaxPriceChange("")
        assertNull(repository.lastCategory)
        assertNull(repository.lastMaxPrice)
        assertEquals(listOf(1, 2, 3), viewModel.visibleIds())
    }

    @Test
    fun invalidMaxPriceIsFlaggedAndNotSentToTheBackend() {
        val repository = FakeCarRepository(listOf(polo))
        val viewModel = viewModel(repository)

        viewModel.onMaxPriceChange("abc")

        assertTrue(viewModel.uiState.value.isMaxPriceInvalid)
        assertNull(repository.lastMaxPrice)
    }

    @Test
    fun grantedPermissionAddsDistancesAndSortsNearestFirst() {
        val viewModel = viewModel()

        viewModel.onLocationPermissionResult(true)
        viewModel.onSortByDistanceChange(true)

        val state = viewModel.uiState.value
        assertEquals(LocationStatus.AVAILABLE, state.locationStatus)
        // Tesla is at the phone's position, the Polo is about 40 km away and the Niro has no location.
        assertEquals(listOf(2, 1, 3), viewModel.visibleIds())
        assertEquals(0.0, state.visibleCars[0].distanceKm!!, 0.001)
        assertEquals(39.5, state.visibleCars[1].distanceKm!!, 0.5)
        assertNull(state.visibleCars[2].distanceKm)
    }

    @Test
    fun withoutSortingTheBackendOrderIsKept() {
        val viewModel = viewModel()

        viewModel.onLocationPermissionResult(true)

        assertEquals(listOf(1, 2, 3), viewModel.visibleIds())
        assertTrue(viewModel.uiState.value.visibleCars.first().distanceKm != null)
    }

    @Test
    fun deniedPermissionKeepsTheListWithoutDistances() {
        val location = FakeLocationProvider(breda)
        val viewModel = viewModel(location = location)

        viewModel.onLocationPermissionResult(false)

        val state = viewModel.uiState.value
        assertEquals(LocationStatus.DENIED, state.locationStatus)
        assertEquals(0, location.calls)
        assertEquals(listOf(1, 2, 3), viewModel.visibleIds())
        assertTrue(state.visibleCars.all { it.distanceKm == null })
    }

    @Test
    fun grantedPermissionWithoutPositionIsUnavailable() {
        val viewModel = viewModel(location = FakeLocationProvider(null))

        viewModel.onLocationPermissionResult(true)

        assertEquals(LocationStatus.UNAVAILABLE, viewModel.uiState.value.locationStatus)
        assertNull(viewModel.uiState.value.userLocation)
    }

    @Test
    fun backendErrorMessageIsShown() {
        val repository = FakeCarRepository().apply { failure = ApiException(400, "Bad filter") }
        val viewModel = viewModel(repository)

        assertEquals(UserMessage(R.string.error_server_message, "Bad filter"), viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun noConnectionShowsTheNoConnectionMessage() {
        val repository = FakeCarRepository().apply { failure = IOException("offline") }
        val viewModel = viewModel(repository)

        assertEquals(UserMessage(R.string.error_no_connection), viewModel.uiState.value.error)
    }

    @Test
    fun onlyOwnersCanAddCars() {
        assertTrue(viewModel(session = session(Role.OWNER)).uiState.value.canAddCars)
        assertFalse(viewModel(session = session(Role.RENTER)).uiState.value.canAddCars)
    }
}
