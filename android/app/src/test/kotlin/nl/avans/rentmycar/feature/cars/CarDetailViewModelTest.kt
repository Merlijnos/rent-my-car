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

@OptIn(ExperimentalCoroutinesApi::class)
class CarDetailViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val car = testCar(1, ownerId = 1, location = GeoPoint(51.5719, 4.7683))

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        userId: Int = 1,
        repository: FakeCarRepository = FakeCarRepository(listOf(car)),
        location: FakeLocationProvider = FakeLocationProvider(GeoPoint(51.6978, 5.3037)),
    ): CarDetailViewModel {
        val session = Session(
            FakeSessionStorage(AuthSession("jwt", SessionUser(userId, "user", Role.OWNER))),
            CoroutineScope(dispatcher),
        )
        return CarDetailViewModel(1, repository, location, session).also { it.load() }
    }

    @Test
    fun ownerSeesEditAndDelete() {
        assertTrue(viewModel(userId = 1).uiState.value.isOwner)
    }

    @Test
    fun otherUsersDoNotSeeEditAndDelete() {
        assertFalse(viewModel(userId = 2).uiState.value.isOwner)
    }

    @Test
    fun loadFetchesTheCarAndDefaultCosts() {
        val repository = FakeCarRepository(listOf(car))
        val viewModel = viewModel(repository = repository)

        assertEquals(car, viewModel.uiState.value.car)
        assertEquals(15_000, repository.lastKmPerYear)
        assertEquals(15_000, viewModel.uiState.value.costs!!.kmPerYear)
    }

    @Test
    fun calculateUsesTheEnteredKilometers() {
        val repository = FakeCarRepository(listOf(car))
        val viewModel = viewModel(repository = repository)

        viewModel.onKmPerYearChange("20000")
        viewModel.calculateCosts()

        assertEquals(20_000, repository.lastKmPerYear)
    }

    @Test
    fun invalidKilometersAreRejectedWithoutCallingTheBackend() {
        val repository = FakeCarRepository(listOf(car))
        val viewModel = viewModel(repository = repository)

        viewModel.onKmPerYearChange("-5")
        viewModel.calculateCosts()

        assertTrue(viewModel.uiState.value.isKmPerYearInvalid)
        assertEquals(15_000, repository.lastKmPerYear)
    }

    @Test
    fun distanceIsShownOnlyWhenLocationIsKnown() {
        val viewModel = viewModel()
        assertNull(viewModel.uiState.value.distanceKm)

        viewModel.onLocationPermissionResult(true)

        assertEquals(39.5, viewModel.uiState.value.distanceKm!!, 2.0)
    }

    @Test
    fun deniedPermissionLeavesDistanceEmpty() {
        val location = FakeLocationProvider(GeoPoint(51.0, 5.0))
        val viewModel = viewModel(location = location)

        viewModel.onLocationPermissionResult(false)

        assertNull(viewModel.uiState.value.distanceKm)
        assertEquals(0, location.calls)
    }

    @Test
    fun deleteAsksForConfirmationFirst() {
        val repository = FakeCarRepository(listOf(car))
        val viewModel = viewModel(repository = repository)

        viewModel.onDeleteClick()
        assertTrue(viewModel.uiState.value.showDeleteDialog)
        assertEquals(1, repository.cars.size)

        viewModel.onDeleteDismiss()
        assertFalse(viewModel.uiState.value.showDeleteDialog)
        assertEquals(1, repository.cars.size)
    }

    @Test
    fun confirmedDeleteRemovesTheCar() {
        val repository = FakeCarRepository(listOf(car))
        val viewModel = viewModel(repository = repository)

        viewModel.onDeleteClick()
        viewModel.onDeleteConfirm()

        assertTrue(viewModel.uiState.value.isDeleted)
        assertTrue(repository.cars.isEmpty())
    }

    @Test
    fun deleteRefusedByTheBackendShowsItsMessage() {
        val repository = FakeCarRepository(listOf(car)).apply {
            deleteFailure = ApiException(400, "Car 1 still has active reservations")
        }
        val viewModel = viewModel(repository = repository)

        viewModel.onDeleteClick()
        viewModel.onDeleteConfirm()

        val state = viewModel.uiState.value
        assertFalse(state.isDeleted)
        assertFalse(state.showDeleteDialog)
        assertEquals(UserMessage(R.string.error_server_message, "Car 1 still has active reservations"), state.deleteError)
    }

    @Test
    fun missingCarShowsNotFoundMessage() {
        val viewModel = viewModel(repository = FakeCarRepository())

        assertNull(viewModel.uiState.value.car)
        assertEquals(UserMessage(R.string.error_server_message, "Car 1 not found"), viewModel.uiState.value.error)
    }
}
