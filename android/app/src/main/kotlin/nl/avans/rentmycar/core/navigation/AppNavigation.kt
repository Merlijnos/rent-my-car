package nl.avans.rentmycar.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import nl.avans.rentmycar.R
import nl.avans.rentmycar.core.session.SessionState
import nl.avans.rentmycar.core.ui.theme.RentMyCarTheme
import nl.avans.rentmycar.feature.auth.LoginScreen
import nl.avans.rentmycar.feature.cars.CarDetailScreen
import nl.avans.rentmycar.feature.cars.CarFormScreen
import nl.avans.rentmycar.feature.cars.CarListScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun AppRoot(viewModel: AppViewModel = koinViewModel()) {
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    RentMyCarTheme {
        when (sessionState) {
            SessionState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            else -> AppShell(
                isLoggedIn = sessionState is SessionState.LoggedIn,
                onLogout = viewModel::logout,
            )
        }
    }
}

/**
 * The frame around every screen: top bar plus the back stack. The session decides the root:
 * logged out always shows [Login], logging in replaces it with [CarList].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppShell(isLoggedIn: Boolean, onLogout: () -> Unit) {
    val backStack = rememberNavBackStack(if (isLoggedIn) CarList else Login)

    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) {
            backStack.replaceWith(Login)
        } else if (backStack.lastOrNull() == Login) {
            backStack.replaceWith(CarList)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(backStack.lastOrNull().titleRes())) },
                navigationIcon = {
                    if (backStack.size > 1) {
                        IconButton(onClick = { backStack.removeLastOrNull() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                        }
                    }
                },
                actions = {
                    if (isLoggedIn) {
                        TextButton(onClick = onLogout) { Text(stringResource(R.string.logout)) }
                    }
                },
            )
        },
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.padding(innerPadding),
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<Login> { LoginScreen() }
                entry<CarList> {
                    CarListScreen(
                        onCarClick = { carId -> backStack.add(CarDetail(carId)) },
                        onAddCar = { backStack.add(CarForm()) },
                    )
                }
                entry<CarDetail> { key ->
                    CarDetailScreen(
                        carId = key.carId,
                        onEdit = { backStack.add(CarForm(key.carId)) },
                        onDeleted = { backStack.removeLastOrNull() },
                    )
                }
                entry<CarForm> { key ->
                    CarFormScreen(carId = key.carId, onSaved = { backStack.removeLastOrNull() })
                }
            },
        )
    }
}

private fun NavBackStack<NavKey>.replaceWith(key: NavKey) {
    clear()
    add(key)
}
