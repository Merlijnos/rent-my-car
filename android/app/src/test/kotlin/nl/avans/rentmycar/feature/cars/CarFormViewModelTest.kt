package nl.avans.rentmycar.feature.cars

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import nl.avans.rentmycar.R
import nl.avans.rentmycar.feature.cars.location.AddressResult
import nl.avans.rentmycar.feature.cars.location.GeoPoint
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CarFormViewModelTest {
    private val repository = FakeCarRepository()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(addressResult: AddressResult = AddressResult.NotFound) =
        CarFormViewModel(null, repository, FakeLocationProvider(null), FakeAddressLookup(addressResult))

    @Test
    fun `empty brand shows an error and nothing is sent`() {
        val viewModel = viewModel()

        viewModel.save()

        assertEquals(R.string.form_error_brand, viewModel.uiState.value.errors.brand)
        assertNull(repository.lastRequest)
    }

    @Test
    fun `address found sets the location and the place name`() {
        val breda = GeoPoint(51.5885, 4.7761)
        val viewModel = viewModel(AddressResult.Found(breda, "Breda, Nederland"))
        viewModel.onAddressChange("Grote Markt, Breda")

        viewModel.onFindAddress()

        assertEquals(breda, viewModel.uiState.value.location)
        assertEquals("Breda, Nederland", viewModel.uiState.value.placeName)
    }

    @Test
    fun `address not found shows an error and sets no location`() {
        val viewModel = viewModel(AddressResult.NotFound)
        viewModel.onAddressChange("qqqq")

        viewModel.onFindAddress()

        assertEquals(R.string.form_address_not_found, viewModel.uiState.value.locationError)
        assertNull(viewModel.uiState.value.location)
    }
}
