package nl.avans.rentmycar

import android.app.Application
import nl.avans.rentmycar.core.di.coreModule
import nl.avans.rentmycar.feature.auth.authModule
import nl.avans.rentmycar.feature.cars.carsModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class RentMyCarApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@RentMyCarApplication)
            modules(coreModule, authModule, carsModule)
        }
    }
}
