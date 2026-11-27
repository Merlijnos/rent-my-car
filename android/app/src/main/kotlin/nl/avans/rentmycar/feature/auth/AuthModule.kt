package nl.avans.rentmycar.feature.auth

import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val authModule = module {
    singleOf(::AuthRepository)
    viewModelOf(::LoginViewModel)
}
