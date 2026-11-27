package nl.avans.rentmycar.feature.cars

import nl.avans.rentmycar.feature.cars.location.AddressLookup
import nl.avans.rentmycar.feature.cars.location.FusedLocationProvider
import nl.avans.rentmycar.feature.cars.location.GeocoderAddressLookup
import nl.avans.rentmycar.feature.cars.location.LocationProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val carsModule = module {
    single<CarRepository> { ApiCarRepository(get()) }
    single<LocationProvider> { FusedLocationProvider(androidContext()) }
    single<AddressLookup> { GeocoderAddressLookup(androidContext()) }
    viewModelOf(::CarListViewModel)
    viewModel { params -> CarDetailViewModel(params.get(), get(), get(), get()) }
    viewModel { params -> CarFormViewModel(params.getOrNull(), get(), get(), get()) }
}
