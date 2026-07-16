# api-bird-orchestator-microservice

Java 17 Spring Boot 3.2.5 (WebFlux) microservice for orchestrating bird detection and classification workflows. Reactive stack with R2DBC (PostgreSQL, pooled), RabbitMQ (reactor-rabbitmq), and AWS S3 SDK v2 (async).

## Architecture

Clean Architecture with reactive programming:

```
Domain (pure Java) → Application (use cases) → Infrastructure (adapters)
```

- **Domain**: Pure Java entities, enums, ports (repository, storage, messaging, result broker), use-case contracts, exceptions
- **Application**: DTOs (records for requests, Lombok `@Data`/`@Builder` for responses), and use cases for both the inbound HTTP flow (`ProcessBirdImageUseCase`, `GetImageStatusUseCase`, `GetImageEventUseCase`, `BirdMapInformationUseCase`) and inbound RabbitMQ event handling (`ProcessDetectionResultUseCase`, `ProcessClassificationResultUseCase`, `CleanupRejectedImagesUseCase`)
- **Infrastructure**: Controllers, R2DBC adapters, S3 adapter, RabbitMQ producer/consumers, in-memory result broker, scheduler, config, species cache, security-headers filter

All service/controller/port methods return `Mono<T>` or `Flux<T>`.

### Caching

Spring's `@Cacheable` (Caffeine provider, async cache mode, `@EnableCaching` on `Application`) backs two independent caches, each on its own bean so calls always go through the Spring proxy:

- **Species lookups** (`speciesByScientificName`, `speciesById`) — `species` is small, rarely-changing
  reference data seeded via `db/` SQL. `SpecieCacheService`
  (`infrastructure/persistence/cache/SpecieCacheService.java`) wraps
  `ISpecieR2DBCRepository.findByScientificName`/`findById`, plus a batched `findAllByIds` (resolves
  cache hits from the Caffeine cache directly and issues a single `findAllById` query for the misses,
  for the "N rows → N species" fan-out in map/sighting-list queries) that writes fresh entries back
  into the `speciesById` cache; `ImageEventRepository` depends on this service instead of the raw
  R2DBC repository.
- **Presigned S3 URLs** (`presignedImageUrls`) — `S3ImageStorageAdapter.generatePresignedUrl` is
  cached directly (keyed on S3 key + requested duration), so repeated thumbnail requests for the same
  photo (map pins, sighting lists) skip both the DB-adjacent blocking `S3Presigner` call and the
  `boundedElastic` thread hop.

Both are sized/expired via per-cache Caffeine specs built in
`infrastructure/configuration/CacheConfig.java` (not Spring Boot's `spring.cache.caffeine.spec`
autoconfiguration, which can only apply one spec to every cache name): `cache.species.spec` (default
`maximumSize=500,expireAfterWrite=30m`, override `CACHE_SPECIES_SPEC`) and `cache.presigned-url.spec`
(default `maximumSize=1000,expireAfterWrite=25m`, override `CACHE_PRESIGNED_URL_SPEC`). The
presigned-url TTL is intentionally shorter than the 30-minute duration callers request — otherwise a
cached entry could be served after its embedded S3 signature already expired. Neither cache has active
invalidation; both rely purely on TTL expiry.

## Message Flow

The flow is synchronous-looking from the client's point of view: `POST /bird/detect` waits (up to a timeout) for the async pipeline to resolve before responding, using an in-memory result broker to bridge the HTTP request to the RabbitMQ consumers.

1. **POST `/bird/detect`** (multipart: image `FilePart` + `userId` UUID + optional `latitude`/`longitude`) → `BirdController` validates the request (`jakarta.validation`, `-180..180`/`-90..90` bounds on the coordinates) then delegates to `ProcessBirdImageUseCase.execute()`:
   - uploads image to S3 (bounded read, aborts past 10 MB)
   - saves `ImageEvent(status=PROCESSING)` with the optional lat/lng
   - publishes `BirdObserved` to exchange `bird_detection.exchange` with routing key `bird_detection.pending`
   - awaits `IImageEventResultBroker.awaitResult(imageEventId, 6s timeout)`, blocking the reactive chain until the pipeline completes or the timeout elapses (falls back to the original `PROCESSING` `ImageEvent` on timeout)
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

# R2DBC connection pool (optional, sane defaults below)
DB_R2DBC_POOL_INITIAL_SIZE=5
DB_R2DBC_POOL_MAX_SIZE=20
DB_R2DBC_POOL_MAX_IDLE_TIME=30m

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
CACHE_SPECIES_SPEC=maximumSize=500,expireAfterWrite=30m         # Caffeine spec for the species lookup caches
CACHE_PRESIGNED_URL_SPEC=maximumSize=1000,expireAfterWrite=25m  # Caffeine spec for the presigned-S3-URL cache
```

### Run Locally
```bash
# Start dependencies (PostgreSQL, RabbitMQ, MinIO) externally
mvn spring-boot:run
# App runs on http://localhost:8081
```

### Run with Docker

`docker-compose.yml` in this repo brings up all five pieces of the pipeline — this service,
`detection`, `classification`, `postgres`, and `rabbitmq` — as separate containers. `detection` and
`classification` build from their own sibling repos (`../api-bird-detection-microservice`,
`../api-bird-classification-microservice`) rather than being bundled into this image, so the three
repos need to be checked out side by side (the `bird-dex` workspace layout) for it to work:

```bash
# from inside api-bird-orchestator-microservice/
cp .env.example .env               # fill in AWS_S3_* credentials
# download the detection + classification .pkl models — see DOCKER.md §1 for links/checksums
docker compose up --build
```

Full instructions (model downloads with checksums, secrets, port table, health checks, known
limitations) are in [`DOCKER.md`](DOCKER.md).

### Build & Test
```bash
mvn compile           # Compile only
mvn test              # Unit tests (JUnit 5 + Reactor Test + Mockito), instruments coverage via JaCoCo
mvn verify            # Runs tests + enforces an 80% JaCoCo instruction-coverage floor + OWASP Dependency-Check
mvn package           # Build JAR (runs tests)
mvn test -Dtest=BirdControllerTest  # Single test
```

CI (`.github/workflows/ci.yml`) runs `mvn verify` on every pull request: unit tests, the JaCoCo 80%
coverage gate, and OWASP Dependency-Check (fails the build on a High/Critical CVE, CVSS ≥ 7, in any
dependency; suppressions live in `doc/suppressions/dependency-check-suppressions.xml`). Set the
`NVD_API_KEY` repo secret to avoid heavy rate-limiting on Dependency-Check's NVD database sync.

## API

### Detect Bird
```http
POST /bird/detect
Content-Type: multipart/form-data

image: <file>
userId: <uuid>
longitude: <decimal, optional, -180..180>
latitude: <decimal, optional, -90..90>
```

The request holds open (up to a 6s internal timeout) while detection — and, if a bird is found, classification — resolves through RabbitMQ, so the response can already reflect the final state. `userId` and out-of-range coordinates are validated up front (`400` via `GlobalExceptionHandler` on failure).

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

### Get Image Status
```http
GET /bird/image-events/{imageEventId}/status
```
Re-fetches an `ImageEvent` by id for clients that don't want to hold the detect request open.

**Response** (`200 OK`, `data`):
```json
{
  "imageEventId": "uuid",
  "status": "DONE",
  "specieId": "uuid",
  "scientificName": "Turdus migratorius",
  "commonName": null,
  "specieConfidence": 0.93,
  "failureReason": null
}
```

### Get Map Sightings
```http
GET /bird/map?minLat=<decimal>&maxLat=<decimal>&minLng=<decimal>&maxLng=<decimal>
```
Returns completed (`DONE`) sightings within the given lat/lng bounding box, each with a presigned S3 thumbnail URL — backs the map view.

**Response** (`200 OK`, `data`): array of
```json
{
  "scientificName": "Turdus migratorius",
  "commonName": null,
  "thumbnailUrl": "https://...",
  "imageEventId": "uuid",
  "speciesId": "uuid",
  "latitude": 4.6097,
  "longitude": -74.0817
}
```

### Get User's Bird Sightings
```http
GET /users/{userId}/birds
```
Returns all of a user's sightings, each with a presigned S3 thumbnail URL.

**Response** (`200 OK`, `data`): array of
```json
{
  "imageEventId": "uuid",
  "specieId": "uuid",
  "scientificName": "Turdus migratorius",
  "commonName": null,
  "thumbnailUrl": "https://...",
  "status": "DONE"
}
```

### Health Check
```http
GET /actuator/health
GET /actuator/info
```

### API Documentation (OpenAPI / Swagger UI)
```http
GET /v3/api-docs       # raw OpenAPI 3 spec (JSON)
GET /swagger-ui.html   # interactive Swagger UI
```
Generated from annotations on `BirdController`/`UserController` and the DTOs (`springdoc-openapi-starter-webflux-ui`); top-level info comes from `infrastructure/configuration/OpenApiConfig`.

## Project Structure

```
src/main/java/com/brayanpv/app/
├── Application.java
├── application/
│   ├── dto/request/ImageUploadRequest.java          # record: FilePart, userId, optional lat/lng (bean-validated)
│   ├── dto/response/
│   │   ├── ImageUploadResponse.java
│   │   ├── ImageStatusResponse.java
│   │   ├── MapSightingResponse.java
│   │   ├── BirdSightingResponse.java
│   │   └── GenericResponse.java
│   └── usecase/
│       ├── ProcessBirdImageUseCase.java              # upload → save ImageEvent(PROCESSING) → publish BirdObserved → await result broker (6s)
│       ├── GetImageStatusUseCase.java                # re-fetches an ImageEvent by id
│       ├── GetImageEventUseCase.java                 # all ImageEvents for a user
│       ├── BirdMapInformationUseCase.java             # DONE sightings within a lat/lng bounding box + presigned thumbnails
│       ├── ProcessDetectionResultUseCase.java        # updates detection status; if bird, publishes ClassificationRequested; if not, completes the result broker
│       ├── ProcessClassificationResultUseCase.java   # updates classification status; if FAILED, publishes ManualClassificationRequested; else completes the result broker
│       └── CleanupRejectedImagesUseCase.java         # finds NOT_A_BIRD events, deletes S3 object, marks EXPIRED
├── domain/
│   ├── model/
│   │   ├── BirdObserved.java
│   │   ├── BirdDetectionResult.java
│   │   ├── BirdClassificationResult.java
│   │   ├── ClassificationRequested.java
│   │   ├── ManualClassificationRequested.java
│   │   ├── ImageEvent.java
│   │   ├── GeoLocation.java
│   │   ├── MapSighting.java
│   │   └── enums/ImageStatus.java
│   ├── exception/
│   │   ├── DeserializationException.java
│   │   ├── ImageEventNotFoundException.java
│   │   └── SpecieNotFoundException.java
│   ├── repository/IImageEventRepository.java         # save, updateDetection, updateClassification, findByStatus, markExpired, findById, findByUserId, findDoneSightingsInBounds
│   ├── storage/IImageStoragePort.java
│   ├── messaging/
│   │   ├── IEventPublisherPort.java
│   │   └── IImageEventResultBroker.java
│   └── usecase/contracts/
│       ├── IProcessBirdImageUseCase.java
│       ├── IGetImageStatusUseCase.java
│       ├── IGetImageEventUseCase.java
│       ├── IBirdMapInformationUseCase.java
│       ├── IProcessDetectionResultUseCase.java
│       ├── IProcessClassificationResultUseCase.java
│       └── ICleanupRejectedImagesUseCase.java
├── infrastructure/
│   ├── web/
│   │   ├── contracts/IBirdController.java, IUserController.java
│   │   ├── implementations/BirdController.java       # /bird/detect, /bird/image-events/{id}/status, /bird/map
│   │   ├── implementations/UserController.java        # /users/{userId}/birds
│   │   └── filter/SecurityHeadersWebFilter.java       # OWASP baseline response headers (nosniff, CSP, HSTS, deny-framing)
│   ├── storage/S3ImageStorageAdapter.java
│   ├── persistence/
│   │   ├── adapter/ImageEventRepository.java
│   │   ├── cache/SpecieCacheService.java
│   │   ├── repository/IImageEventR2DBCRepository.java, ISpecieR2DBCRepository.java
│   │   ├── entity/ImageEventEntity.java, SpecieEntity.java
│   │   └── projection/MapSightingProjection.java
│   ├── messaging/
│   │   ├── producer/RabbitEventPublisher.java
│   │   ├── consumer/BirdDetectionEventConsumer.java
│   │   ├── consumer/BirdClassificationEventConsumer.java
│   │   └── broker/InMemoryImageEventResultBroker.java
│   ├── scheduling/RejectedImageCleanupScheduler.java
│   ├── configuration/
│   │   ├── RabbitMQConfig.java
│   │   ├── RabbitTopologyInitializer.java
│   │   ├── S3ConnectionConfiguration.java
│   │   ├── CacheConfig.java
│   │   └── OpenApiConfig.java                         # springdoc OpenAPI Info bean (/v3/api-docs, /swagger-ui.html)
│   ├── handle/GlobalExceptionHandler.java             # ConstraintViolationException → 400, generic RuntimeException → 400
│   └── mapper/ImageEventMapper.java
└── test/                                              # mirrors main; unit tests per use case, consumer, adapter, filter (Mockito + Reactor Test)
```

## Configuration

`src/main/resources/application.yml`:
- `server.port: 8081`
- `spring.r2dbc.url` / pool (`spring.r2dbc.pool.*`) — PostgreSQL via env vars, pooled via `r2dbc-pool`
- `management.endpoints.web.exposure.include: health,info`
- `aws.s3` — bucket, credentials via env vars
- `cleanup.rejected-images.fixed-delay-ms` — interval for `RejectedImageCleanupScheduler`
- `cache.species.spec` / `cache.presigned-url.spec` — size/TTL for the species and presigned-S3-URL caches, wired up by `CacheConfig`
- `springdoc.api-docs.path` / `springdoc.swagger-ui.path` — OpenAPI JSON / Swagger UI paths (defaults `/v3/api-docs`, `/swagger-ui.html`)
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
- [ ] Species cache (`SpecieCacheService`) has no active invalidation — relies purely on a 30-minute TTL, so an in-place edit to a `species` row can be served stale for up to that long
- [ ] Presigned-URL cache assumes every caller requests roughly the same ~30 min duration for a given S3 key; a call site requesting a much shorter duration for the same key could receive an already-expired cached URL
- [ ] `IImageEventResultBroker` is in-memory only (`ConcurrentHashMap` + `Sinks.One`) — won't coordinate correctly if the orchestrator is horizontally scaled
- [ ] No consumer for `bird_classification.manual.queue` in this repo (presumably a manual-review tool elsewhere)
- [ ] No integration tests with Testcontainers (PostgreSQL, RabbitMQ, MinIO) — none yet, `pom.xml` has no Testcontainers dependency

## Tech Stack

- **Framework**: Spring Boot 3.2.5 (WebFlux)
- **Database**: PostgreSQL + R2DBC (pooled via `r2dbc-pool`)
- **Messaging**: RabbitMQ + reactor-rabbitmq
- **Storage**: AWS S3 SDK v2 (async)
- **Caching**: Spring Cache abstraction + Caffeine, async mode (species lookups, presigned URLs)
- **Build**: Maven
- **Testing**: JUnit 5, Reactor Test, Mockito
- **Coverage / Security**: JaCoCo (80% instruction-coverage floor on business logic), OWASP Dependency-Check (fails on CVSS ≥ 7 CVEs)
- **CI**: GitHub Actions (`.github/workflows/ci.yml`) — `mvn verify` on every pull request
- **Observability**: Spring Boot Actuator
- **API docs**: springdoc-openapi (OpenAPI 3 + Swagger UI, WebFlux)
