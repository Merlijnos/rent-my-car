package nl.avans.rentmycar.feature.cars

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import nl.avans.rentmycar.R
import nl.avans.rentmycar.core.common.UserMessage
import nl.avans.rentmycar.core.network.ApiException
import nl.avans.rentmycar.feature.cars.location.AddressResult
import nl.avans.rentmycar.feature.cars.location.GeoPoint
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CarFormViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun createViewModel(
        repository: FakeCarRepository = FakeCarRepository(),
        location: FakeLocationProvider = FakeLocationProvider(),
        addressLookup: FakeAddressLookup = FakeAddressLookup(),
    ) = CarFormViewModel(null, repository, location, addressLookup)

    private fun CarFormViewModel.fillValidForm() {
        onBrandChange("Volkswagen")
        onModelChange("Polo 1.0 TSI")
        onPricePerDaypartChange("15")
        onFixedCostsPerYearChange("2400")
        onConsumptionPer100KmChange("5,4")
    }

    @Test
    fun emptyFormShowsAnErrorPerRequiredField() {
        val repository = FakeCarRepository()
        val viewModel = createViewModel(repository)

        viewModel.save()

        val errors = viewModel.uiState.value.errors
        assertEquals(R.string.form_error_brand, errors.brand)
        assertEquals(R.string.form_error_model, errors.model)
        assertEquals(R.string.form_error_amount, errors.pricePerDaypart)
        assertEquals(R.string.form_error_amount, errors.fixedCostsPerYear)
        assertEquals(R.string.form_error_consumption, errors.consumptionPer100Km)
        assertNull(repository.lastRequest)
    }

    @Test
    fun editingAFieldClearsItsError() {
        val viewModel = createViewModel()
        viewModel.save()

        viewModel.onBrandChange("Kia")

        assertNull(viewModel.uiState.value.errors.brand)
        assertNotNull(viewModel.uiState.value.errors.model)
    }

    @Test
    fun brandAndModelHaveAMaximumOf50Characters() {
        val viewModel = createViewModel()
        viewModel.fillValidForm()
        viewModel.onBrandChange("x".repeat(51))

        viewModel.save()

        assertEquals(R.string.form_error_brand, viewModel.uiState.value.errors.brand)
    }

    @Test
    fun numbersMustBeInRange() {
        val viewModel = createViewModel()
        viewModel.fillValidForm()
        viewModel.onPricePerDaypartChange("-1")
        viewModel.onFixedCostsPerYearChange("abc")
        viewModel.onConsumptionPer100KmChange("0")

        viewModel.save()

        val errors = viewModel.uiState.value.errors
        assertEquals(R.string.form_error_amount, errors.pricePerDaypart)
        assertEquals(R.string.form_error_amount, errors.fixedCostsPerYear)
        assertEquals(R.string.form_error_consumption, errors.consumptionPer100Km)
    }

    @Test
    fun validFormSendsARequestAndIsSaved() {
        val repository = FakeCarRepository()
        val addressLookup = FakeAddressLookup(AddressResult.Found(GeoPoint(51.5719, 4.7683), "Breda, Nederland"))
        val viewModel = createViewModel(repository, addressLookup = addressLookup)
        viewModel.fillValidForm()
        viewModel.onAddressChange("Grote Markt, Breda")
        viewModel.onFindAddress()

        viewModel.save()

        assertEquals(
            CarRequest(
                category = CarCategory.ICE,
                brand = "Volkswagen",
                model = "Polo 1.0 TSI",
                pricePerDaypart = 15.0,
                fixedCostsPerYear = 2400.0,
                consumptionPer100Km = 5.4,
                fuelType = FuelType.PETROL,
                latitude = 51.5719,
                longitude = 4.7683,
            ),
            repository.lastRequest,
        )
        assertTrue(viewModel.uiState.value.isSaved)
    }

    @Test
    fun fuelTypeIsOnlySentForIceCars() {
        val repository = FakeCarRepository()
        val viewModel = createViewModel(repository)
        viewModel.fillValidForm()
        viewModel.onCategoryChange(CarCategory.BEV)

        viewModel.save()

        assertEquals(CarCategory.BEV, repository.lastRequest!!.category)
        assertNull(repository.lastRequest!!.fuelType)
        assertNull(repository.lastRequest!!.latitude)
    }

    @Test
    fun serverErrorIsShownAndFormIsNotSaved() {
        val repository = FakeCarRepository().apply { failure = ApiException(403, "Only OWNER can do this") }
        val viewModel = createViewModel(repository)
        viewModel.fillValidForm()

        viewModel.save()

        assertEquals(UserMessage(R.string.error_server_message, "Only OWNER can do this"), viewModel.uiState.value.saveError)
        assertFalse(viewModel.uiState.value.isSaved)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun editLoadsTheCarAndUpdatesIt() {
        val repository = FakeCarRepository(listOf(testCar(7, "Kia", "Niro EV", CarCategory.BEV, 30.0, location = GeoPoint(51.6, 4.8))))
        val viewModel = CarFormViewModel(7, repository, FakeLocationProvider(), FakeAddressLookup(placeName = "Breda, Nederland"))

        val loaded = viewModel.uiState.value
        assertEquals("Kia", loaded.brand)
        assertEquals("30.0", loaded.pricePerDaypart)
        assertEquals(GeoPoint(51.6, 4.8), loaded.location)
        assertEquals("Breda, Nederland", loaded.placeName)

        viewModel.onModelChange("Niro")
        viewModel.save()

        assertEquals("Niro", repository.lastRequest!!.model)
        assertTrue(viewModel.uiState.value.isSaved)
    }

    @Test
    fun findingAnAddressSetsTheLocationAndShowsThePlaceName() {
        val addressLookup = FakeAddressLookup(AddressResult.Found(GeoPoint(51.5885, 4.7761), "Breda, Nederland"))
        val viewModel = createViewModel(addressLookup = addressLookup)

        viewModel.onAddressChange(" Grote Markt, Breda ")
        viewModel.onFindAddress()

        val state = viewModel.uiState.value
        assertEquals("Grote Markt, Breda", addressLookup.lastAddress)
        assertEquals(GeoPoint(51.5885, 4.7761), state.location)
        assertEquals("Breda, Nederland", state.placeName)
        assertNull(state.locationError)
        assertFalse(state.isLocating)
    }

    @Test
    fun addressNotFoundShowsAMessageAndKeepsTheOldLocation() {
        val addressLookup = FakeAddressLookup(AddressResult.Found(GeoPoint(51.5, 4.7), "Breda, Nederland"))
        val viewModel = createViewModel(addressLookup = addressLookup)
        viewModel.onAddressChange("Breda")
        viewModel.onFindAddress()

        addressLookup.result = AddressResult.NotFound
        viewModel.onAddressChange("qqqq")
        viewModel.onFindAddress()

        assertEquals(R.string.form_address_not_found, viewModel.uiState.value.locationError)
        assertEquals(GeoPoint(51.5, 4.7), viewModel.uiState.value.location)
        assertFalse(viewModel.uiState.value.isLocating)
    }

    @Test
    fun failedLookupShowsAConnectionMessage() {
        val viewModel = createViewModel(addressLookup = FakeAddressLookup(AddressResult.Failed))
        viewModel.onAddressChange("Breda")

        viewModel.onFindAddress()

        assertEquals(R.string.form_address_failed, viewModel.uiState.value.locationError)
        assertNull(viewModel.uiState.value.location)
    }

    @Test
    fun blankAddressIsNotLookedUp() {
        val addressLookup = FakeAddressLookup()
        val viewModel = createViewModel(addressLookup = addressLookup)
        viewModel.onAddressChange("  ")

        viewModel.onFindAddress()

        assertEquals(R.string.form_address_empty, viewModel.uiState.value.locationError)
        assertNull(addressLookup.lastAddress)
    }

    @Test
    fun typingAgainClearsTheLookupError() {
        val viewModel = createViewModel(addressLookup = FakeAddressLookup(AddressResult.NotFound))
        viewModel.onAddressChange("qqqq")
        viewModel.onFindAddress()

        viewModel.onAddressChange("Breda")

        assertNull(viewModel.uiState.value.locationError)
    }

    @Test
    fun useMyLocationSetsTheLocationAndShowsThePlaceName() {
        val viewModel = createViewModel(
            location = FakeLocationProvider(GeoPoint(51.57194, 4.76831)),
            addressLookup = FakeAddressLookup(placeName = "Breda, Nederland"),
        )

        viewModel.onLocationPermissionResult(true)

        assertEquals(GeoPoint(51.57194, 4.76831), viewModel.uiState.value.location)
        assertEquals("Breda, Nederland", viewModel.uiState.value.placeName)
        assertFalse(viewModel.uiState.value.isLocating)
    }

    @Test
    fun useMyLocationWithoutAPlaceNameStillSetsTheLocation() {
        val viewModel = createViewModel(location = FakeLocationProvider(GeoPoint(51.57194, 4.76831)))

        viewModel.onLocationPermissionResult(true)

        assertEquals(GeoPoint(51.57194, 4.76831), viewModel.uiState.value.location)
        assertNull(viewModel.uiState.value.placeName)
    }

    @Test
    fun deniedLocationPermissionShowsAMessageAndKeepsTheAddressSearchAvailable() {
        val viewModel = createViewModel()

        viewModel.onLocationPermissionResult(false)

        assertEquals(R.string.form_location_denied, viewModel.uiState.value.locationError)
        assertNull(viewModel.uiState.value.location)
    }

    @Test
    fun noPositionShowsAMessage() {
        val viewModel = createViewModel(location = FakeLocationProvider(null))

        viewModel.onLocationPermissionResult(true)

        assertEquals(R.string.form_location_unavailable, viewModel.uiState.value.locationError)
    }
}
