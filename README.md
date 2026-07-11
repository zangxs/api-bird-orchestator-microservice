# api-bird-orchestator-microservice

Java 17 Spring Boot 3.2.5 (WebFlux) microservice for orchestrating bird detection and classification workflows. Reactive stack with R2DBC (PostgreSQL), RabbitMQ (reactor-rabbitmq), and AWS S3 SDK v2 (async).

## Architecture

Clean Architecture with reactive programming:

```
Domain (pure Java) → Application (services / use cases) → Infrastructure (adapters)
```

- **Domain**: Pure Java entities, enums, ports (repository, storage, messaging, result broker), use-case contracts, exceptions
- **Application**: DTOs (records), the original service contract/implementation (`IBirdService`/`BirdService`, inbound HTTP flow), and use cases for inbound RabbitMQ event handling (`ProcessDetectionResultUseCase`, `ProcessClassificationResultUseCase`, `CleanupRejectedImagesUseCase`)
- **Infrastructure**: Controllers, R2DBC adapters, S3 adapter, RabbitMQ producer/consumers, in-memory result broker, scheduler, config

All service/controller/port methods return `Mono<T>` or `Flux<T>`.

## Message Flow

The flow is now synchronous-looking from the client's point of view: `POST /bird/detect` waits (up to a timeout) for the async pipeline to resolve before responding, using an in-memory result broker to bridge the HTTP request to the RabbitMQ consumers.

1. **POST `/bird/detect`** (multipart: image `FilePart` + `userId` UUID) → `BirdService.processImage()`:
   - uploads image to S3
   - saves `ImageEvent(status=PROCESSING)`
   - publishes `BirdObserved` to exchange `bird_detection.exchange` with routing key `bird_detection.pending`
   - registers a wait on `IImageEventResultBroker.awaitResult(imageEventId, 6s timeout)` and blocks the reactive chain until the pipeline completes or the timeout elapses (falls back to the original `PROCESSING` `ImageEvent` on timeout)
2. **Detection microservice** (external, Python) consumes `bird_detection.pending.queue` → downloads from S3 → runs the model → publishes `BirdDetectionResult` with routing key `bird_detection.result`
3. `BirdDetectionEventConsumer` consumes `bird_detection.resultado.queue` → delegates to `ProcessDetectionResultUseCase`:
   - `IImageEventRepository.updateDetection(imageEventId, isBird, confidence)` → status becomes `BIRD_DETECTED` or `NOT_A_BIRD`
   - if it *is* a bird → publishes `ClassificationRequested` with routing key `bird_classification.pending` to hand off to the classification service
   - if it's *not* a bird → calls `IImageEventResultBroker.complete(imageEvent)`, unblocking the original HTTP request with the `NOT_A_BIRD` result
4. **Classification microservice** (external, Python — WIP) consumes `bird_classification.queue` → resolves the species → publishes `BirdClassificationResult` with routing key `bird_classification.result`
5. `BirdClassificationEventConsumer` consumes `bird_classification.result.queue` → delegates to `ProcessClassificationResultUseCase`:
   - `IImageEventRepository.updateClassification(imageEventId, scientificName, specieConfidence, failureReason)`
   - if classification failed (status `FAILED`) → publishes `ManualClassificationRequested` with routing key `bird_classification.manual.pending`, for manual review
   - otherwise → calls `IImageEventResultBroker.complete(imageEvent)`, unblocking the original HTTP request with the final species result
6. A scheduled job, `RejectedImageCleanupScheduler` (`cleanup.rejected-images.fixed-delay-ms`, default 1h), runs `CleanupRejectedImagesUseCase`: finds `ImageEvent`s with status `NOT_A_BIRD`, deletes their S3 object, and marks them `EXPIRED`

### Image status lifecycle

`PROCESSING → BIRD_DETECTED | NOT_A_BIRD → IDENTIFYING → DONE | FAILED`, plus a replace-photo sub-flow (`PENDING_REPLACE_CONFIRMATION → REPLACED | REPLACE_REJECTED`) and `EXPIRED` (terminal state after cleanup of `NOT_A_BIRD` images).

## Quick Start

### Prerequisites
- Java 17, Maven 3.9+
- PostgreSQL 15+
- RabbitMQ 3.12+
- S3-compatible storage (MinIO / AWS S3)

### Environment Variables
```bash
# PostgreSQL
DB_BIRD_DEX_HOST=localhost
DB_BIRD_DEX_PORT=5432
DB_BIRD_DEX_NAME=bird_dex
DB_BIRD_DEX_USER=bird_user
DB_BIRD_DEX_PASSWORD=bird_password

# S3
AWS_S3_BUCKET_NAME=bird-dex-bucket
AWS_S3_ACCESS_KEY=minioadmin
AWS_S3_SECRET_KEY=minioadmin

# RabbitMQ
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USER=user
RABBITMQ_PASSWORD=password

# Optional
CLEANUP_REJECTED_IMAGES_FIXED_DELAY_MS=3600000   # rejected-image cleanup interval, default 1h
```

### Run Locally
```bash
# Start dependencies (PostgreSQL, RabbitMQ, MinIO) externally
mvn spring-boot:run
# App runs on http://localhost:8080
```

### Build & Test
```bash
mvn compile           # Compile only
mvn test              # Unit tests (JUnit 5 + Reactor Test + Mockito)
mvn package           # Build JAR (runs tests)
mvn test -Dtest=BirdControllerTest  # Single test
```

## API

### Detect Bird
```http
POST /bird/detect
Content-Type: multipart/form-data

image: <file>
userId: <uuid>
```

The request holds open (up to a 6s internal timeout) while detection — and, if a bird is found, classification — resolves through RabbitMQ, so the response can already reflect the final state.

**Response** (`200 OK`):
```json
{
  "dateTime": "2026-01-15T10:30:00Z",
  "code": 200,
  "data": {
    "imageEventId": "uuid",
    "status": "DONE",
    "specieId": "uuid",
    "speciesConfidence": 0.93
  }
}
```
`status` may be `PROCESSING` (pipeline didn't finish within the timeout), `NOT_A_BIRD`, or a classification outcome (`DONE`/`FAILED`); `specieId`/`speciesConfidence` are `null` until classification completes.

### Health Check
```http
GET /actuator/health
GET /actuator/info
```

## Project Structure

```
src/main/java/com/brayanpv/app/
├── Application.java
├── application/
│   ├── dto/request/ImageUploadRequest.java
│   ├── dto/response/ImageUploadResponse.java
│   ├── dto/response/GenericResponse.java
│   ├── service/
│   │   ├── contracts/IBirdService.java
│   │   └── implementations/BirdService.java
│   └── usecase/
│       ├── ProcessDetectionResultUseCase.java
│       ├── ProcessClassificationResultUseCase.java
│       └── CleanupRejectedImagesUseCase.java
├── domain/
│   ├── model/
│   │   ├── BirdObserved.java
│   │   ├── BirdDetectionResult.java
│   │   ├── BirdClassificationResult.java
│   │   ├── ClassificationRequested.java
│   │   ├── ManualClassificationRequested.java
│   │   ├── ImageEvent.java
│   │   ├── Specie.java
│   │   └── enums/ImageStatus.java
│   ├── exception/
│   │   ├── DeserializationException.java
│   │   ├── ImageEventNotFoundException.java
│   │   └── SpecieNotFoundException.java
│   ├── repository/
│   │   ├── IImageEventRepository.java
│   │   └── ISpecieRepository.java
│   ├── storage/IImageStoragePort.java
│   ├── messaging/
│   │   ├── IEventPublisherPort.java
│   │   └── IImageEventResultBroker.java
│   └── usecase/contracts/
│       ├── IProcessDetectionResultUseCase.java
│       ├── IProcessClassificationResultUseCase.java
│       └── ICleanupRejectedImagesUseCase.java
├── infrastructure/
│   ├── web/
│   │   ├── contracts/IBirdController.java
│   │   └── implementations/BirdController.java
│   ├── storage/S3ImageStorageAdapter.java
│   ├── persistence/
│   │   ├── adapter/ImageEventRepository.java
│   │   ├── adapter/SpecieRepository.java
│   │   ├── repository/IImageEventR2DBCRepository.java
│   │   ├── repository/ISpecieR2DBCRepository.java
│   │   ├── entity/ImageEventEntity.java
│   │   └── entity/SpecieEntity.java
│   ├── messaging/
│   │   ├── producer/RabbitEventPublisher.java
│   │   ├── consumer/BirdDetectionEventConsumer.java
│   │   ├── consumer/BirdClassificationEventConsumer.java
│   │   └── broker/InMemoryImageEventResultBroker.java
│   ├── scheduling/RejectedImageCleanupScheduler.java
│   ├── configuration/
│   │   ├── RabbitMQConfig.java
│   │   ├── RabbitTopologyInitializer.java
│   │   └── S3ConnectionConfiguration.java
│   ├── handle/GlobalExceptionHandler.java
│   └── mapper/ImageEventMapper.java
└── test/
    ├── ApplicationTest.java
    ├── application/service/implementations/BirdServiceTest.java
    └── infrastructure/web/implementations/BirdControllerTest.java
```

## Configuration

`src/main/resources/application.yml`:
- `server.port: 8080`
- `spring.r2dbc.url` — PostgreSQL via env vars
- `management.endpoints.web.exposure.include: health,info`
- `aws.s3` — bucket, credentials via env vars
- `cleanup.rejected-images.fixed-delay-ms` — interval for `RejectedImageCleanupScheduler`
- `rabbitmq.*` — exchange, queues, routing keys, all bound in `RabbitMQConfig.java` against exchange `bird_detection.exchange`:

| Key | Value |
|---|---|
| `queue` / `routing-key` | `bird_detection.pending.queue` / `bird_detection.pending` |
| `result-queue` / `result-routing-key` | `bird_detection.resultado.queue` / `bird_detection.result` |
| `classification-queue` / `classification-routing-key` | `bird_classification.queue` / `bird_classification.pending` |
| `classification-result-queue` / `result-classification-routing-key` | `bird_classification.result.queue` / `bird_classification.result` |
| `manual-classification-queue` / `manual-classification-routing-key` | `bird_classification.manual.queue` / `bird_classification.manual.pending` |

## Known Gaps / TODO

- [ ] Classification microservice's `classificate_bird()` is still a stub — no model inference wired up yet on the Python side
- [ ] `SpecieRepository.findByScientificName()` is unimplemented (returns `null`, not `Mono.empty()`)
- [ ] Integration tests with Testcontainers (PostgreSQL, RabbitMQ, MinIO) — none yet, `pom.xml` has no Testcontainers dependency
- [ ] Request validation on `ImageUploadRequest`
- [ ] OpenAPI/Swagger configuration
- [ ] Global exception handler tests
- [ ] `docker-compose.yml` for local dependencies

## Tech Stack

- **Framework**: Spring Boot 3.2.5 (WebFlux)
- **Database**: PostgreSQL + R2DBC
- **Messaging**: RabbitMQ + reactor-rabbitmq
- **Storage**: AWS S3 SDK v2 (async)
- **Build**: Maven
- **Testing**: JUnit 5, Reactor Test, Mockito
- **Observability**: Spring Boot Actuator
