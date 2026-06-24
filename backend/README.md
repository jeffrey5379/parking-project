# Parking Lot Service (Backend)

Spring Boot 3.3.4 REST API managing a 27-spot parking lot (9 Small, 9 Medium, 9 Large).

## Domain rules

- A vehicle parks in a spot of its own size or any larger size.  
  Motorcycle → Small/Medium/Large, Car → Medium/Large, Truck → Large only.
- The service always picks the **smallest compatible free spot** to avoid wasting large spots.
- **Relocation**: if no compatible spot is free but a compatible spot is occupied by a smaller vehicle that could fit in a smaller free spot, that vehicle is relocated. The destination slot immediately shows `RELOCATING` status; a SQS message triggers the physical move (simulated 5–10 s delay) after which the spot becomes `OCCUPIED`.
- **Optimistic locking**: every `ParkingSpot` row has a JPA `@Version` column. Concurrent park requests that race to claim the same spot get `OptimisticLockingFailureException` at commit time and are retried up to 10 times.
- **Virtual threads**: `parkConcurrently` spins up one virtual thread per vehicle using `Executors.newVirtualThreadPerTaskExecutor()`.

## Prerequisites

- JDK 21
- Maven 3.8+
- Docker (for ElasticMQ — SQS emulator used in local development)

## Run locally

```bash
# start ElasticMQ (from project root)
docker-compose up -d

# start the backend
mvn spring-boot:run
```

API on `http://localhost:8080`, H2 console on `http://localhost:8080/h2-console`.

## Run tests

Tests mock SQS (`parking.sqs.enabled=false`) and use an in-memory H2 database — no Docker required.

```bash
mvn test
```

## Build Docker image

```bash
docker build -t parking-lot-service:latest .
```

## API reference

| Method | Path                            | Description                                       |
| ------ | ------------------------------- | ------------------------------------------------- |
| GET    | `/api/parking/spots`            | All 27 spots with current state                   |
| GET    | `/api/parking/availability`     | Free/occupied counts per size                     |
| POST   | `/api/parking/park`             | Park a vehicle (auto spot selection + relocation) |
| POST   | `/api/parking/park-concurrent`  | Park multiple vehicles concurrently               |
| POST   | `/api/parking/spots/{id}/clear` | Free a specific spot                              |
| POST   | `/api/parking/reset`            | Free every spot                                   |

### POST /api/parking/park

```json
{ "vehicleType": "CAR" }
```

`vehicleType`: `MOTORCYCLE` | `CAR` | `TRUCK`. Returns `201 Created` with the assigned spot, or `409 Conflict` if no spot is available even after attempting relocation.

### POST /api/parking/park-concurrent

```json
{ "motorcycleCount": 3, "carCount": 5, "truckCount": 1 }
```

Returns `{ "parked": 8, "failed": 1 }`.

## Project structure

```
src/main/java/com/parking/app/
  ParkingLotApplication.java
  DataInitializer.java           Seeds 27 spots on first startup
  model/
    SpotSize.java                SMALL / MEDIUM / LARGE + canFit()
    SpotStatus.java              FREE / OCCUPIED / RELOCATING
    VehicleType.java             MOTORCYCLE / CAR / TRUCK → required SpotSize
    ParkingSpot.java             JPA entity with @Version optimistic locking
  repository/
    ParkingSpotRepository.java   Spring Data JPA
  service/
    ParkingLotService.java       Core logic: allocation, relocation, concurrency
  sqs/
    RelocationMessageConsumer.java  @SqsListener — completes relocation after delay
  controller/
    ParkingController.java
  config/
    AsyncConfig.java             Virtual thread executor
    CorsConfig.java              CORS from property (local only)
    SqsConfig.java               Ensures SQS queue exists on startup
  dto/
    ParkVehicleRequest.java
    ConcurrentParkRequest.java
    ConcurrentParkResult.java
    RelocationMessage.java
    AvailabilitySummary.java
    ParkingSpotResponse.java
  exception/
    NoAvailableSpotException.java
    SpotNotFoundException.java
    SpotAlreadyFreeException.java
    GlobalExceptionHandler.java
```
