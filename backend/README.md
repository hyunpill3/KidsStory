# KidsStory Backend

Spring Boot 3 (Java 21) REST API and AI video-generation pipeline. Single
deployable — the async video pipeline runs in-process (`@Async`) rather than
as a separate worker, so there's no message broker to run.

For the overall project (frontend, user flow, the AI video feature itself),
see the [root README](../README.md).

## Stack

- Java 21, Maven (wrapper included — no local Maven install needed)
- Spring Boot 3.4, Spring Data JPA, Flyway (schema migrations)
- PostgreSQL
- Replicate (Kling v1.6) for image-to-video generation, called via raw HTTP
  (no official Replicate Java SDK)
- Lombok, `spring-dotenv` (auto-loads `.env` outside docker-compose)

## Prerequisites

- **JDK 21** — check with `java -version`. If Lombok-generated methods
  (`cannot find symbol: method getFoo()`) fail to compile, your JDK is
  probably a different major version than what compiled the stale
  `target/`, or Spring Boot's managed Lombok version is behind what your
  specific JDK patch needs (see Troubleshooting below).
- **PostgreSQL** reachable at `localhost:5432` (either the project's own
  `docker compose up postgres` from `infrastructure/`, or your own local
  Postgres install).

## First-time setup

1. Copy the env template and fill in `VIDEO_GEN_API_KEY`:
   ```bash
   cp .env.example .env
   ```
2. Create the `kidsstory` role and database (skip if using
   `docker compose up postgres`, which creates these automatically):
   ```bash
   psql -U postgres -h localhost -c "CREATE ROLE kidsstory WITH LOGIN PASSWORD 'kidsstory';"
   psql -U postgres -h localhost -c "CREATE DATABASE kidsstory OWNER kidsstory;"
   ```
   (Run these as two **separate** commands — `CREATE DATABASE` cannot run
   inside the same transaction/batch as another statement.)
3. Run it:
   ```bash
   ./mvnw.cmd spring-boot:run        # Windows
   ./mvnw spring-boot:run            # macOS/Linux
   ```
   Flyway creates the schema automatically on first boot — no manual
   migration step.
4. Check it's up: `http://localhost:8000/health` → `{"status":"ok"}`

## Running in VS Code

Install the **Extension Pack for Java** (Microsoft). A `launch.json` is
already committed (both here and at the repo root, so it works whichever
folder you open) — just press **F5**.

## Running tests

```bash
./mvnw.cmd test
```

## Project layout

```
config/      CORS, static /media mapping, async executor, scheduling, storage backend selection
controller/  REST endpoints (ProjectController) + AuthController (ported, deliberately unmounted)
dto/         Request/response records - the REST contract the frontend depends on
entity/      JPA entities + enums (enum JSON values must match frontend/src/types/index.ts exactly)
repository/  Spring Data JPA interfaces
service/     Business logic, including the AI pipeline stages (see below)
exception/   ApiException + GlobalExceptionHandler
```

### The AI pipeline (`service/`)

`VideoGenerationService` orchestrates these stages in order, mirroring the
original design (`docs/superpowers/specs/2026-08-25-java-backend-rewrite-design.md`):

1. `PhotoAnalysisService` — stub
2. `StoryGenerationService` — stub
3. `SceneSplittingService` — real logic, no AI
4. `SceneRenderingService` — **real**, calls Replicate
5. `NarrationService` — stub
6. `AudioMixingService` — stub
7. `VideoCompositionService` — **real**, shells out to ffmpeg

Only style (not age/voice/language) currently affects the generated video,
since narration/story-generation are still stubs — see the pipeline stage
list above.

## Troubleshooting

**Lombok methods "cannot find symbol" during compile, but only sometimes**
Lombok periodically needs a point release to track javac's internal APIs
across JDK patch updates; an older pinned version can silently skip
annotation processing (no error, the getters/setters just never get
generated) against a newer JDK 21 patch. `pom.xml` pins an explicit recent
Lombok version rather than trusting Spring Boot's managed version - if this
recurs on a newer JDK patch, bump that version.

**`class file has wrong version X.0, should be Y.0`**
Stale `target/` compiled under a different JDK than the one currently
running the build (e.g. switched from JDK 19 to 21, or two build tools
targeting the same `target/` directory concurrently). Run
`./mvnw.cmd clean` — but **not** while another process (VS Code's Java
extension, another terminal) has the app running from this same `target/`
directory, or you'll pull `target/classes` out from under it and get a
`NoClassDefFoundError` on the next request instead.

**`FATAL: password authentication failed for user "kidsstory"`**
Postgres is reachable but the `kidsstory` role/database don't exist yet (or
have a different password) in whichever Postgres instance
`SPRING_DATASOURCE_URL` points at. See step 2 of First-time setup.

**F5 in VS Code: `Could not find or load main class`**
The Java language server's build state is stale or incomplete - run
`./mvnw.cmd compile` to confirm Maven itself builds cleanly, then
`Ctrl+Shift+P` → "Java: Clean Java Language Server Workspace" → reload.

**Replicate call fails with `402 Payment Required` / "Insufficient credit"**
Your Replicate account needs a payment method and credit -
[replicate.com/account/billing](https://replicate.com/account/billing).
Kling v1.6 Standard is $0.05/sec ($0.50 for a free-tier 10s video). Credit
top-ups can take a few minutes to propagate before predictions succeed.
