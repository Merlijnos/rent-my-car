package nl.avans.rentmycar.core.di

import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import nl.avans.rentmycar.BuildConfig
import nl.avans.rentmycar.core.navigation.AppViewModel
import nl.avans.rentmycar.core.network.createHttpClient
import nl.avans.rentmycar.core.session.DataStoreSessionStorage
import nl.avans.rentmycar.core.session.Session
import nl.avans.rentmycar.core.session.SessionStorage
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val coreModule = module {
    single<SessionStorage> { DataStoreSessionStorage(androidContext()) }
    single { Session(get(), CoroutineScope(SupervisorJob() + Dispatchers.Default)) }
    single { createHttpClient(OkHttp.create(), BuildConfig.BASE_URL, get()) }
    viewModelOf(::AppViewModel)
}
