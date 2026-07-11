# api-bird-orchestator-microservice

Java 17 Spring Boot 3.2.5 (WebFlux) microservice for orchestrating bird detection workflows. Reactive stack with R2DBC (PostgreSQL), RabbitMQ (reactor-rabbitmq), and AWS S3 SDK v2 (async).

## Architecture

Clean Architecture with reactive programming:

```
Domain (pure Java) → Application (use cases) → Infrastructure (adapters)
```

- **Domain**: Pure Java entities, enums, ports (interfaces), exceptions
- **Application**: DTOs (records), service contracts & implementations
- **Infrastructure**: Controllers, R2DBC adapters, S3 adapter, RabbitMQ producer/consumer, config

All service/controller/port methods return `Mono<T>` or `Flux<T>`.

## Message Flow

1. **POST `/bird/detect`** (multipart: image `FilePart` + `userId` UUID)
2. `BirdService.processImage()` → uploads image to S3 → saves `ImageEvent(status=PROCESSING)` → publishes `BirdObserved` to RabbitMQ exchange `deteccion`
3. **Python detector** (external) consumes `deteccion` → downloads from S3 → runs model → publishes `BirdDetectionResult` to `deteccion-resultado`
4. `BirdDetectionEventConsumer` consumes `deteccion-resultado` → calls `IImageEventRepository.updateDetection(imageEventId, isBird, confidence)` → updates status to `BIRD_DETECTED` or `NOT_A_BIRD`
5. *(Future)* If `BIRD_DETECTED` → publish to `clasificacion` queue for classification pipeline

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

**Response** (`200 OK`):
```json
{
  "dateTime": "2026-01-15T10:30:00Z",
  "code": 200,
  "data": {
    "imageEventId": "uuid",
    "status": "PROCESSING"
  }
}
```

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
│   └── service/
│       ├── contracts/IBirdService.java
│       └── implementations/BirdService.java
├── domain/
│   ├── model/
│   │   ├── BirdObserved.java
│   │   ├── BirdDetectionResult.java
│   │   ├── ImageEvent.java
│   │   └── enums/ImageStatus.java
│   ├── exception/
│   │   ├── DeserializationException.java
│   │   └── ImageEventNotFoundException.java
│   ├── repository/IImageEventRepository.java
│   ├── storage/IImageStoragePort.java
│   └── messaging/IEventPublisherPort.java
├── infrastructure/
│   ├── web/
│   │   ├── contracts/IBirdController.java
│   │   └── implementations/BirdController.java
│   ├── storage/S3ImageStorageAdapter.java
│   ├── persistence/
│   │   ├── adapter/ImageEventRepository.java
│   │   ├── repository/IImageEventR2DBCRepository.java
│   │   └── entity/ImageEventEntity.java
│   ├── messaging/
│   │   ├── producer/implementations/BirdObservedEventPublisher.java
│   │   └── consumer/BirdDetectionEventConsumer.java
│   ├── configuration/
│   │   ├── RabbitMQConfig.java
│   │   ├── RabbitTopologyInitializer.java
│   │   └── S3ConnectionConfiguration.java
│   ├── handle/GlobalExceptionHandler.java
│   └── mapper/ImageEventMapper.java
└── test/
    ├── ApplicationTest.java
    └── infrastructure/web/implementations/BirdControllerTest.java
```

## Configuration

`src/main/resources/application.yml`:
- `server.port: 8080`
- `spring.r2dbc.url` — PostgreSQL via env vars
- `management.endpoints.web.exposure.include: health,info`
- `aws.s3` — bucket, credentials via env vars
- `rabbitmq.*` — exchange, queues, routing keys (configured in `RabbitMQConfig.java`)

## Known Gaps / TODO

- [ ] Integration tests with Testcontainers (PostgreSQL, RabbitMQ, MinIO)
- [ ] Request validation on `ImageUploadRequest`
- [ ] OpenAPI/Swagger configuration
- [ ] Global exception handler tests
- [ ] Classification pipeline (step 5 in message flow)
- [ ] `docker-compose.yml` for local dependencies
- [ ] Add Testcontainers to `pom.xml`

## Tech Stack

- **Framework**: Spring Boot 3.2.5 (WebFlux)
- **Database**: PostgreSQL + R2DBC
- **Messaging**: RabbitMQ + reactor-rabbitmq
- **Storage**: AWS S3 SDK v2 (async)
- **Build**: Maven
- **Testing**: JUnit 5, Reactor Test, Mockito
- **Observability**: Spring Boot Actuator