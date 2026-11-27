# Rent my car: Android app

Android client for the Rent my car backend (Kotlin, Jetpack Compose, Material 3).
It lives next to the Ktor backend in this repository and talks to it over HTTP.

## Run

1. Start the backend from the repository root. It listens on `http://localhost:8080`.

   ```sh
   ./kotlin run
   ```

2. Start an emulator (an Android Studio device, or `~/Library/Android/sdk/emulator/emulator -avd Pixel_10a`).
   The emulator reaches the backend on your computer at `http://10.0.2.2:8080`.
   That address is `BASE_URL` in `app/build.gradle.kts` (exposed as `BuildConfig.BASE_URL`).
   The debug build allows plain HTTP for `10.0.2.2` and `localhost` only
   (`app/src/debug/res/xml/network_security_config.xml`).
3. Open the `android` folder in Android Studio and run the `app` configuration, or from a terminal:

   ```sh
   cd android
   ./gradlew installDebug
   ```

Log in with a seeded user: `paul` / `paul123` or `sara` / `sara123` (OWNER), `lisa` / `lisa123` or `tom` / `tom123` (RENTER).
The build needs JDK 21 (`JAVA_HOME`) and the Android SDK (`local.properties` with `sdk.dir`, Android Studio creates it).

## Test and check

```sh
cd android
./gradlew testDebugUnitTest          # unit tests, run on the JVM
./gradlew connectedDebugAndroidTest  # UI and integration tests, need a running emulator or device
./gradlew assembleDebug lint
```

The integration tests that call the real API need the backend running (step 1) with the seeded database.
They create their own cars and leave a few test cars behind until the backend restarts.

## Tests

Results of the last run on the Pixel_10a emulator (Android 17) with the backend running:
73 unit tests and 35 UI and integration tests (29 UI, 6 integration), all passing; the 52 backend tests were not changed in this step.

Unit tests (`app/src/test`, JVM, 73 tests)

| Test class | Tests | What it proves |
| --- | --- | --- |
| `DistanceCalculatorTest` | 5 | Haversine gives known distances (same point is 0, Amsterdam to Rotterdam, symmetric, one degree of latitude, half the equator). |
| `CarSearchTest` | 5 | Search matches brand or model, all words must match, distance is only set when both locations are known, nearest first and unknown last. |
| `CarFormValidatorTest` | 4 | A valid form becomes a trimmed request, blank or bad numbers give an error per field, the chosen location ends up as latitude and longitude in the request. |
| `CarListViewModelTest` | 11 | Category and max price go to the backend, invalid max price is not sent, search runs on the phone, granted permission adds distances and sorts, denied permission keeps the list working, backend and connection errors, only owners can add cars. |
| `CarFormViewModelTest` | 17 | Errors per required field, a valid form is sent and saved, fuel type only for ICE, edit updates, server error keeps the form, address search with a fake `AddressLookup` (found, not found, failed, blank), "use my location" with a fake GPS, denied permission and no position. |
| `CarDetailViewModelTest` | 11 | Owner-only buttons, cost calculation and invalid kilometers, distance only with a location, delete with confirmation and the backend's message when it refuses. |
| `CarJsonMappingTest` | 9 | JSON maps to `Car` (with and without location, unknown fields ignored), and the repository against a mock engine: filter parameters, costs, POST body, DELETE 204 and the 400 message. |
| `LoginViewModelTest`, `HttpClientFactoryTest`, `UserMessageTest` | 3, 4, 4 | Login flow, Bearer header and 401 logout, error to message mapping (shared base). |

UI tests (`app/src/androidTest`, Compose UI test on the emulator, 29 tests)

| Test class | Tests | What it proves |
| --- | --- | --- |
| `CarListScreenTest` | 11 | List shows cars, search filters, error with retry, add button for owners only, sort by distance shows the nearest first with "x km away", denied permission banner with settings button, rationale banner. |
| `CarFormScreenTest` | 10 | Empty form shows errors, address field and "Find" button instead of coordinates, the found place is shown as text, address error, fuel type only for combustion cars, "use my current location" button and its error text. |
| `CarDetailScreenTest` | 8 | Edit and delete only for the owner, delete dialog asks first and cancel closes it, confirming deletes, costs are shown, the "Route" button only for a car with a location. |

Integration tests (`CarsBackendIntegrationTest`, 6 tests, real backend at `10.0.2.2:8080`)
read the seeded cars, filter on category, unknown car gives an error, an owner (`paul`) creates, updates, reads back and deletes
a car with latitude and longitude, an invalid latitude gives a 400 with a message, and deleting a car that `lisa` reserved gives
the backend's 400 message "still has active reservations".

The Fused Location code itself (`FusedLocationProvider`) is not covered by an automated test, because it needs the real Google Play services.
Manual GPS test on the emulator: start the app, log in, tap "Allow location" and accept; in the emulator's Extended controls
(Location) send a point near Breda (51.5719, 4.7683); the cars show "x km away" and "Sort by distance" becomes active.
Deny the permission once to see the fallback: the list works, no distances. In the form, "Use my current location" sets the car's location and shows the place name.
Address search (`GeocoderAddressLookup`) is also tested by hand: type "Grote Markt, Breda" and tap "Find"; the form shows "Found: Breda, Netherlands". It needs internet on the phone.
The "Route" button opens Google Maps (or the browser) outside the app, so that is tested by hand too.

## AI use

I generated parts of the code (the app, the backend location change and the tests) with Claude Code, an AI coding assistant.
I chose the scope and the architecture (MVVM, Ktor, Koin, Navigation 3, Android only, GPS as sensor) and gave the instructions step by step.
I reviewed each result and made every step pass a build and a test run on the JVM or the emulator, and I made the commits myself.
The rules that matter (search, distance, validation) are the small classes under "Domain" above, and I can explain them.

## Decisions

- Session token in plain DataStore: simple, and the app has no sensitive data besides the token. The upgrade is `EncryptedSharedPreferences` or an Android Keystore key, and backups are off (`allowBackup="false"`).
- `FusedLocationProvider` wraps Google's callback API in `suspendCancellableCoroutine`, so a ViewModel can simply call `currentLocation()`; cancelling the coroutine cancels the location request.
- Category and max price run on the backend (query parameters, less data over the network); search by text runs on the phone, because it should react on every key press without a request.
- Distance is derived data: computed on the phone from the user's location and the car's coordinates, never stored or sent to the server.
- `targetSdk` stays 36: with 37 the emulator could not reach `10.0.2.2`. The Compose BOM stays 2026.09, because 2026.11 needs `compileSdk` 37.1, which is not installed. Both lint warnings are switched off in `app/build.gradle.kts` for that reason.
- The ViewModels have no mapping layer between JSON and domain model: `Car` is `@Serializable` and used everywhere.

## Structure

Package root `nl.avans.rentmycar`.

```
core/
  common/      UserMessage: turns an error into a string resource for the user
  di/          coreModule: Session, HttpClient, AppViewModel (Koin)
  navigation/  Destinations (typed keys), AppNavigation (Scaffold, top bar, back stack)
  network/     createHttpClient (Ktor), ApiException, apiCall
  session/     Session (token and logged-in user, kept in DataStore)
  ui/theme/    RentMyCarTheme
feature/
  auth/        LoginScreen, LoginViewModel, AuthRepository, authModule
  cars/        car list, detail and form (screens, ViewModels, CarRepository, carsModule),
               CarSearch and CarFormValidator (domain rules), CarPreviews (@Preview functions)
    location/  LocationProvider + FusedLocationProvider (GPS), AddressLookup + GeocoderAddressLookup (address to position), WalkingRoute (opens Google Maps), DistanceCalculator (haversine), permission helper
```

Each screen follows the same layers, from the user down to the backend:

| Layer | Class (cars feature) | Job |
| --- | --- | --- |
| UI | `CarListScreen` / `CarListContent` | Shows one `UiState`, sends user actions up. No logic. |
| ViewModel | `CarListViewModel` | Holds the `StateFlow<UiState>`, reacts to actions, calls the layers below. |
| Domain (service) | `CarSearch`, `CarFormValidator`, `DistanceCalculator` | The business rules: search, sort by distance, form validation, haversine. Plain classes without Android or network, so they have their own unit tests. |
| Data | `CarRepository` (interface), `ApiCarRepository` | Calls the API through the shared `HttpClient`, returns `Result`. |

The Android architecture guide calls the domain layer optional. It is used here on purpose: the rules would otherwise sit in
the ViewModel or in the `UiState`, where they are harder to read and to test. It stays small: one class per group of rules,
no use case per button. `Car` is both the JSON model and the domain model, because the backend fields are exactly what the screens need.

Why MVVM with one `UiState` per screen: the ViewModel survives a rotation, so the screen keeps its state; the composables only
show a state they are given (state hoisting), so they can be previewed and tested with any state; and the ViewModel can be unit
tested on the JVM with a fake repository and a fake `LocationProvider`.
`CarPreviews.kt` has `@Preview` functions (list loaded, empty and error, detail, form with errors) that serve as the screen designs.

The session decides where the user lands: logged out is always the login screen,
a successful login replaces it with the car list. The `HttpClient` adds `Authorization: Bearer <token>`
to every request, and logs out when an authenticated request gets a 401.
Errors from the backend (`{"error": "..."}`) arrive as `ApiException`; `Throwable.toUserMessage()` is the single place that
maps them, and "no connection", to a readable text.

The car list shows distances only when the user allows location. The permission is asked after a short explanation,
and when it is denied the list still works without distances. ViewModels use the `LocationProvider` interface,
so unit tests use a fake instead of the GPS. The emulator has no real position: set one in the emulator's extended controls (Location).

All UI text is in `res/values/strings.xml` (English) and `res/values-nl/strings.xml` (Dutch).

## Add your feature in 3 steps

1. Destination: add a key to `core/navigation/Destinations.kt` (a `@Serializable` object or data class that implements `NavKey`),
   give it a title in `titleRes()`, and add `entry<YourKey> { YourScreen() }` in `AppNavigation.kt`.
2. Repository: create `feature/yourfeature/YourRepository.kt` that takes the `HttpClient` and wraps each call in `apiCall { ... }`,
   so the ViewModel gets a `Result`.
3. Koin module: create `feature/yourfeature/YourModule.kt` with `singleOf(::YourRepository)` and `viewModelOf(::YourViewModel)`,
   and add it to `modules(...)` in `RentMyCarApplication`.

Screens get the shell's top bar for free. A screen that needs its own floating button can use its own `Scaffold`.
