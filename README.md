# Rent my car

A Ktor Web API for renting out cars.
Cars come in three categories: combustion engine (ICE), battery electric (BEV) and hydrogen fuel cell (FCEV).
Each category calculates its own cost per kilometer and total cost of ownership.

The API uses an H2 in-memory database: nothing to install, and every start begins with the same 8 example cars.
Changes are lost when the server stops.

## Run

```sh
./kotlin run
```

The server listens on `http://localhost:8080`. Windows uses `kotlin.bat` instead of `./kotlin`.
The wrapper downloads the Kotlin toolchain and the JDK on the first run.

Open `requests.http` in IntelliJ to call every endpoint in order.

## Test

```sh
./kotlin test
```

## Endpoints

| Method | Path | Result |
| --- | --- | --- |
| GET | `/cars` | All cars, optionally filtered with `?category=BEV` and `?maxPricePerDaypart=25` |
| GET | `/cars/{id}` | One car, or 404 |
| POST | `/cars` | Creates a car: 201 with the stored car |
| PUT | `/cars/{id}` | Replaces a car: 200, or 404 |
| DELETE | `/cars/{id}` | Deletes a car: 204, or 404 |
| GET | `/cars/{id}/costs?kmPerYear=15000` | Cost per km and total cost of ownership (default 15000 km) |

Invalid input returns 400 and a missing car 404, both with a body like `{"error": "Car 42 not found"}`.

A car in a request body (`fuelType` is PETROL, DIESEL or LPG, and only needed for ICE):

```json
{
  "category": "ICE",
  "brand": "Volkswagen",
  "model": "Polo 1.0 TSI",
  "pricePerDaypart": 15.0,
  "fixedCostsPerYear": 2400.0,
  "consumptionPer100Km": 5.4,
  "fuelType": "PETROL"
}
```

`consumptionPer100Km` is in liters (ICE), kWh (BEV) or kg of hydrogen (FCEV). Prices are in euros.

## Project structure

```
module.yaml            dependencies and build settings
resources/             application.yaml (host, port, database) and logback.xml
src/rentmycar/
  Server.kt            main: reads the config and starts the server
  Application.kt       installs JSON, error handling and the routes
  Database.kt          connects to H2, creates the tables, adds example data
  cars/
    Car.kt             domain: abstract Car with IceCar, BevCar and FcevCar
    CarTable.kt        database table
    CarRepository.kt   reads and writes cars
    CarRoutes.kt       HTTP endpoints
    CarDtos.kt         JSON request and response classes
test/rentmycar/cars/   unit tests (CarTest) and HTTP tests (CarRoutesTest)
```

Each feature gets its own folder with the same four parts: Table, Repository, Routes and Dtos.
Later features follow `cars/`: `users/`, `reservations/` and `search/`.
To add one, create the table in `Database.kt` and call its routes from `routing { }` in `Application.kt`.
