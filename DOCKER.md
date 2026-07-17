# Running the backend with Docker

`docker-compose.yml` in this repo (`api-bird-orchestator-microservice/`) dockerizes all three
backend services plus their `postgres` and `rabbitmq` dependencies. S3 stays external/real AWS —
there's no local MinIO in this compose file.

## 0. Architecture: five containers, three separate images

| Service | Image built from | Notes |
|---|---|---|
| `orchestrator` | `.` (this repo) | Java/Spring WebFlux, entry point |
| `detection` | `../api-bird-detection-microservice` | Python/FastAPI, own repo, own `Dockerfile` |
| `classification` | `../api-bird-classification-microservice` | Python, consumer-only, own repo, own `Dockerfile` |
| `postgres` | `postgres:16-alpine` | seeded from this repo's `scripts/` on first boot |
| `rabbitmq` | `rabbitmq:3.13-management-alpine` | |

**Detection and classification are never built into the orchestrator's image.** Each is its own
independently-versioned repo with its own `Dockerfile`; `docker-compose.yml` here just points
`build:` at their directories as siblings of this one and runs them as separate containers. That
means this only works if all three repos are checked out side by side under the same parent
directory (the `bird-dex` workspace layout described in the workspace-root `CLAUDE.md`):

```
bird-dex/
├── api-bird-orchestator-microservice/    # docker-compose.yml + this file live here
├── api-bird-detection-microservice/
└── api-bird-classification-microservice/
```

Run every command below **from inside `api-bird-orchestator-microservice/`**.

## 1. Model files (not in any repo, not in the Docker images)

`app/ml/*.pkl` in both Python services is gitignored local data and is excluded from the Docker
build context (`.dockerignore`) on purpose — these files are 85–100MB+, too heavy to ship in git or
bake into an image layer.

**Detection model** — hosted publicly on Hugging Face:
<https://huggingface.co/brayanspv/bird_detection_brayanpv>. `fastai`'s `Learner.export()` pickles
the whole `Learner` (model + `DataLoaders` transform pipeline), which is why the Hub's scanner flags
it `Unsafe` (`PAIT-PYTCH-100`/`101` — arbitrary-code-execution-*capable*, not evidence the file is
actually tampered with; this is a near-universal false-positive pattern for fastai exports). The
repo is public/ungated, so plain `curl`/`wget` works with no token:

```bash
mkdir -p ../api-bird-detection-microservice/app/ml
curl -L -o ../api-bird-detection-microservice/app/ml/bird_model_latest.pkl \
  https://huggingface.co/brayanspv/bird_detection_brayanpv/resolve/main/bird_model_latest.pkl

# verify you got the exact file that was uploaded, not something swapped in later at that URL
echo "6603ab02a38a4d5ad56b02ab472a9c51a8ac5744da13fde513ee47eb694ce86b  ../api-bird-detection-microservice/app/ml/bird_model_latest.pkl" | sha256sum -c
```

**Classification model** — hosted publicly on Hugging Face:
<https://huggingface.co/brayanspv/bird_classification/tree/main>.

```bash
mkdir -p ../api-bird-classification-microservice/app/ml
curl -L -o ../api-bird-classification-microservice/app/ml/bird_species_classifier_latest.pkl \
  https://huggingface.co/brayanspv/bird_classification/resolve/main/bird_species_classifier_latest.pkl

# verify you got the exact file that was uploaded, not something swapped in later at that URL
echo "4b93c63d3a9d77b08da0e762c5560eeadcef4cf559947f8889d08680c532782d  ../api-bird-classification-microservice/app/ml/bird_species_classifier_latest.pkl" | sha256sum -c
```

`docker-compose.yml` bind-mounts each `app/ml/` directory read-only into its container. If the file
isn't there, the service fails on startup trying to `load_learner(...)` — that's the intended
signal, not a bug to work around.

## 2. Configure secrets

```bash
cp .env.example .env
```

Fill in `AWS_S3_BUCKET_NAME`, `AWS_S3_ACCESS_KEY`, `AWS_S3_SECRET_KEY` with real values — the
orchestrator, detection, and classification services all fail fast (`docker compose` refuses to
start them) if these are left empty. Postgres/RabbitMQ credentials in `.env.example` already have
working defaults for local use; change them only if you need to.

## 3. Build and run

```bash
docker compose up --build
```

| Service | Port | Notes |
|---|---|---|
| orchestrator | `8081` | `POST /bird/detect`, `GET /bird/image-events/{id}/status`, `GET /bird/map`, `GET /users/{id}/birds` |
| detection | `8000` | `POST /detector` (also consumes off RabbitMQ) |
| classification | — | consumer-only, no HTTP port |
| postgres | `5432` | seeded on first boot from this repo's `scripts/00_extensions.sql`, `01_ddl.sql`, `seed_species_col_500.sql` |
| rabbitmq | `5672` (AMQP), `15672` (management UI) | |

First boot takes longer — Postgres runs its init scripts once (data persists in the `pgdata`
volume after that; delete the volume to reseed from scratch), and the Python services' images
include `torch`/`fastai`, which are large downloads on the first `--build`. Expect `detection` and
`classification` images around **~9GB each** on disk — `pip install torch` pulls the default CUDA
build (bundled `nvidia-*` wheels: cuBLAS, cuDNN, etc.), even though both services only run inference
on CPU; the CUDA libs are dead weight here, not something the code uses. Both services install these
heavy deps from an identical `requirements-ml.txt` (`api-bird-detection-microservice` and
`api-bird-classification-microservice`, versions pinned to match the training venv:
`fastai==2.8.7`, `torch==2.13.0`, `torchvision==0.28.0`) in its own Docker layer *before* each
service's own `requirements.txt` — since the file and the `RUN pip install` step are byte-identical
in both repos, Docker/BuildKit caches and stores that ~8.9GB layer **once** and both images share it
on disk, rather than paying for it twice.

## 4. Verify it's actually working

```bash
docker compose ps                       # all five should show "healthy" or "running"
curl -f http://localhost:8081/actuator/health   # orchestrator: {"status":"UP"}
curl -f http://localhost:8000/                  # detection: {"Hello": "Mundo"}
docker compose logs classification --tail 20    # should show "consumer started" with no traceback
```

`docker compose logs -f <service>` (`orchestrator`, `detection`, `classification`, `postgres`,
`rabbitmq`) is the fastest way to see why a container is unhealthy — the two most common first-run
failures are a missing `.pkl` (§1) and an empty AWS var (§2), and both fail loudly in the relevant
service's logs rather than hanging silently.

To exercise the full pipeline end to end, `POST` a real bird photo:

```bash
curl -X POST http://localhost:8081/bird/detect \
  -F "image=@/path/to/photo.jpg" \
  -F "userId=$(python3 -c 'import uuid; print(uuid.uuid4())')"
```

A response within ~6s means the whole chain worked: orchestrator → S3 → RabbitMQ → detection →
RabbitMQ → orchestrator → (if a bird) RabbitMQ → classification → RabbitMQ → orchestrator → back to
you. `GET /bird/image-events/{imageEventId}/status` (the `id` from that response) re-checks it later
if you got the `PROCESSING` timeout fallback instead.

## 5. Known limitations of this setup

- No auth service — see the root `CLAUDE.md`. Nothing in this compose file changes that.
- `bird_classification.manual.queue` fills up with no consumer behind it (see
  `diagrams/architecture.md` §2) — this compose file doesn't add one.
- Postgres seeds a `user_bird` table via `01_ddl.sql`, but nothing in the orchestrator writes to
  it — it comes up empty and stays empty. Don't take its presence in the schema as a sign that
  sighting history is tracked anywhere; today it isn't.
