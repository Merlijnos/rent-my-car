# Rent my car

A Ktor Web API for renting out cars.
Cars come in three categories: combustion engine (ICE), battery electric (BEV) and hydrogen fuel cell (FCEV).
Each category calculates its own cost per kilometer and total cost of ownership.

Users register as **OWNER** (offer a car) or **RENTER** (rent a car) and log in with JWT,
as covered in lesson 6. Each car is linked to its owner.

The API uses an H2 in-memory database: nothing to install, and every start begins with the same example users and cars.
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

## Seeded users

| Username | Password | Role |
| --- | --- | --- |
| paul | paul123 | OWNER |
| sara | sara123 | OWNER |
| lisa | lisa123 | RENTER |
| tom | tom123 | RENTER |

Lisa already has Paul's Polo from 2026-07-06 through 2026-07-08 (confirmed).
Tom has a pending booking of Sara's Niro EV on 2026-08-10.

## Endpoints

| Method | Path | Auth | Result |
| --- | --- | --- | --- |
| POST | `/users/register` | — | Creates a user (role `OWNER` or `RENTER`): 201 |
| POST | `/auth/login` | — | Returns a JWT: `{"token":"..."}` , or 401 |
| GET | `/users/me` | Bearer | The logged-in user (without password) |
| GET | `/cars` | — | All cars, optionally filtered with `?category=BEV` and `?maxPricePerDaypart=25` |
| GET | `/cars/{id}` | — | One car, or 404 |
| POST | `/cars` | OWNER | Creates a car linked to the logged-in owner: 201 |
| PUT | `/cars/{id}` | OWNER | Replaces own car: 200, or 403/404 |
| DELETE | `/cars/{id}` | OWNER | Deletes own car: 204, or 403/404. 400 while a pending or confirmed reservation exists |
| GET | `/cars/{id}/costs?kmPerYear=15000` | — | Cost per km and total cost of ownership (default 15000 km) |
| POST | `/reservations` | RENTER | Books a car from `startDate` through `endDate` (inclusive): 201 |
| GET | `/reservations` | Bearer | Renter: own bookings. Owner: bookings of own cars. Optional `?status=` and `?carId=` |
| GET | `/reservations/{id}` | Bearer | That booking, if you are the renter or the car's owner, or 403/404 |
| POST | `/reservations/{id}/confirm` | OWNER | Confirms a pending booking of own car: 200 |
| POST | `/reservations/{id}/reject` | OWNER | Rejects a pending booking of own car: 200 |
| POST | `/reservations/{id}/cancel` | Bearer | Renter or car owner cancels a pending or confirmed booking: 200 |

Invalid input returns 400, missing resources 404, wrong credentials 401 and forbidden actions 403,
all with a body like `{"error": "Car 42 not found"}`.

Register:

```json
{
  "username": "marie",
  "password": "marie123",
  "email": "marie@rentmycar.nl",
  "role": "OWNER"
}
```

Login:

```json
{
  "username": "paul",
  "password": "paul123"
}
```

A car in a request body (`fuelType` is PETROL, DIESEL or LPG, and only needed for ICE).
The `ownerId` is taken from the JWT, not from the body:

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

A reservation (dates are `yyyy-MM-dd`, both inclusive). Each day is two dayparts, morning and afternoon,
so the price is `days × 2 × pricePerDaypart`, stored when the booking is created.
A new booking is `PENDING` until the car's owner confirms or rejects it.
Pending and confirmed bookings block those days for the same car:

```json
{
  "carId": 1,
  "startDate": "2026-09-01",
  "endDate": "2026-09-03"
}
```

## Project structure

```
module.yaml            dependencies and build settings
resources/             application.yaml (host, port, database, jwt) and logback.xml
src/rentmycar/
  Server.kt            main: reads the config and starts the server
  Application.kt       installs JSON, JWT auth, error handling and the routes
  Database.kt          connects to H2, creates the tables, adds example data
  auth/                login and JWT (lesson 6 style)
  users/               User domain, table, repository, routes, DTOs
  cars/
    Car.kt             domain: abstract Car with IceCar, BevCar and FcevCar
    CarTable.kt        database table
    CarRepository.kt   interface CarRepository and ExposedCarRepository, which reads and writes cars
    CarRoutes.kt       HTTP endpoints
    CarDtos.kt         JSON request and response classes
  reservations/        Reservation domain, table, repository, routes, DTOs
test/rentmycar/        unit tests and HTTP tests for auth, users, cars and reservations
```

Each feature gets its own folder with the same four parts: Table, Repository, Routes and Dtos.
Later features follow `cars/`: `search/`.
To add one, create the table in `Database.kt` and call its routes from `routing { }` in `Application.kt`.
