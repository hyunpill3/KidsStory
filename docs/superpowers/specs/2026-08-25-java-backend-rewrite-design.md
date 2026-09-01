# KidsStory backend/worker: Python → Java rewrite

Date: 2026-08-25
Status: Approved by user, pending implementation plan

## Context

KidsStory's backend (FastAPI) and worker (Celery) are currently Python. The
user wants them rewritten in Java for a resume/portfolio project. The
frontend (Next.js) and the video-generation feature (Replicate/Kling,
implemented in the Python worker in a prior session) are **not** in scope for
this rewrite — the Java backend must reproduce the same behavior, including
the already-working Replicate integration.

This is a portfolio demo, not a production system with live user data:
there is no need to preserve Alembic migration history or migrate an
existing Postgres database. A fresh schema is fine.

## Decisions (confirmed with user)

1. **Single unified Spring Boot app** — no separate worker process. The REST
   API and the async video-generation pipeline live in one deployable,
   using `@Async` instead of a Celery/Redis broker. This drops Redis and the
   `worker`/`worker-beat` containers from docker-compose entirely.
2. **Maven** as the build tool.
3. **Java 21 LTS.**
4. **Delete** `backend/` and `worker/` (the Python implementations) once the
   Java version is working. They remain recoverable via git history.

## Current architecture (what must be reproduced)

Read in full during a prior session. Summary:

- **Data model** (Postgres, SQLAlchemy today): `users`, `projects`,
  `project_images`, `scenes`, `videos`, `credits` (CreditTransaction),
  `payments`, `subscriptions`. Only `projects`/`project_images`/`scenes`/
  `videos` are touched by active business logic; `users`/`credits`/
  `payments`/`subscriptions` exist in the schema (and in
  `backend/app/models/`) but are **not referenced by any current service
  logic** — the MVP is anonymous-only. Port all of them for schema parity,
  same as today (dead-but-present).
- **Auth**: JWT (python-jose) + bcrypt (passlib), `POST /auth/register`,
  `POST /auth/login`. Defined in `backend/app/api/v1/auth.py` /
  `core/security.py` / `services/user_service.py` but **not mounted** on the
  API router (`backend/app/api/router.py` comments explain this — kept for a
  future signed-in flow). Anonymous identity for the active flow is a
  cookie (`ks_anon_id`) + client IP (`AnonIdentity` in `api/deps.py`), used
  for per-IP/cookie rate limiting (`free_daily_project_limit`).
- **Active REST endpoints** (`backend/app/api/v1/projects.py`):
  - `POST /api/v1/projects/` — create project (validates captcha, daily
    limit), body: `storyPrompt`, `options` (ageGroup/videoLength/style/
    voice/language), `captchaToken`.
  - `POST /api/v1/projects/{id}/upload/` — multipart photo upload (max
    `max_photos_free`=1, `max_upload_bytes`=8MB), generates a thumbnail.
  - `POST /api/v1/projects/{id}/generate/` — sets status QUEUED, enqueues
    the pipeline.
  - `GET /api/v1/projects/{id}/status/` — status, progress, videoUrl,
    errorMessage; frontend polls this every 3s.
  - All responses use camelCase JSON (Python's `CamelModel`); all request/
    response shapes must be byte-for-byte compatible so the frontend needs
    **zero changes**.
- **Captcha**: Cloudflare Turnstile verification (`services/captcha.py`),
  skipped when no secret key configured (dev default).
- **Storage** (`services/storage_service.py`, both backend and worker have a
  copy): `local` (disk under `media/projects/{id}/...`, served via
  `/media` static mount) or `r2` (S3-compatible, boto3). Same dual-backend
  design must be reproduced.
- **Pipeline** (`worker/worker/tasks/pipeline.py`, Celery task): analyze
  photos (stub) → generate story (stub) → split into scenes (stub) → render
  scenes (**real**, Replicate) → narration (stub) → audio mixing (stub) →
  compose final video (**real**, ffmpeg) → upload → notify. Status/progress
  written to `projects.status`/`progress` after each stage (percentages in
  `PROGRESS_BY_STATUS`), polled by the frontend — this must be reproduced
  exactly since the frontend's `GenerationProgress` component depends on the
  exact status enum values.
- **Video generation** (already implemented, must be ported as-is):
  - `scene_rendering.py`: Replicate `kwaivgi/kling-v1.6-standard`
    (image + prompt → video), `client.run()` blocks with SDK-internal
    polling. Motion prompt built from style + scene visual description +
    a fixed cinematic template. `duration=10` (matches free-tier's
    advertised 10s and `SECONDS_PER_SCENE`).
  - `video_composition.py`: ffmpeg concat of scene clips + watermark
    drawtext filter, `subprocess`.
  - `storage_service.load_image_bytes()`: reads local disk (shared volume
    with backend) or HTTP-fetches (R2 public URL).
  - Cost control already in place: 1 photo, 10s video, 1 project/day/IP,
    watermark always on, 24h TTL + periodic purge — unchanged.
- **Scheduled cleanup** (`worker/worker/tasks/cleanup.py`, Celery beat):
  deletes projects (and storage) past `expires_at`.
- **Config**: all via env vars (pydantic-settings today) — same env var
  *names* should carry over where sensible so `.env.example` stays familiar.

## Java design

### Stack

- Java 21, Spring Boot 3.3+, Maven.
- `spring-boot-starter-web`, `spring-boot-starter-data-jpa`,
  `spring-boot-starter-validation`.
- `postgresql` (JDBC driver), `flyway-core` + `flyway-database-postgresql`.
- `io.jsonwebtoken:jjwt-api/-impl/-jackson` (JWT) — no full Spring Security;
  auth stays unmounted/unused exactly like today, so a full security filter
  chain isn't warranted. `spring-security-crypto` for `BCryptPasswordEncoder`
  only.
- `software.amazon.awssdk:s3` (R2/S3 storage backend).
- `net.coobird:thumbnailator` (image thumbnails, replaces Pillow).
- Spring's `RestClient` (sync, Spring 6.1+) for the Replicate HTTP calls —
  no reactive stack needed.
- Lombok (idiomatic for Spring Boot Java, reduces entity/DTO boilerplate).
- `spring-boot-starter-test` (JUnit 5) — default starter; a handful of
  service-level tests are reasonable for a resume-quality repo, but this
  is not a test-heavy rewrite (the Python original had zero tests).

### Package layout

```
com.kidsstory
├── KidsStoryApplication.java
├── config/          WebConfig (CORS, /media static mapping), AsyncConfig (executor), SchedulingConfig
├── controller/       ProjectController, AuthController (unmounted route group, ported as-is)
├── dto/               request/response records (ProjectCreateRequest, ProjectOut, ProjectStatusOut, ...)
├── entity/           User, Project, ProjectImage, Scene, Video, CreditTransaction, Payment, Subscription
├── repository/        Spring Data JPA interfaces
├── service/
│   ├── ProjectService.java        (create/upload/generate/status — mirrors project_service.py)
│   ├── AnonIdentityInterceptor.java
│   ├── CaptchaService.java        (Turnstile)
│   ├── UserService.java           (ported, unused by active routes)
│   ├── StorageService.java        (interface) + LocalStorageService / R2StorageService
│   ├── VideoGenerationService.java (@Async pipeline orchestration, replaces pipeline.py)
│   ├── SceneRenderingService.java  (Replicate call, replaces scene_rendering.py)
│   ├── VideoCompositionService.java (ffmpeg, replaces video_composition.py)
│   ├── PhotoAnalysisService, StoryGenerationService, SceneSplittingService,
│   │   NarrationService, AudioMixingService  (stubs, same fixed/free behavior as today)
│   └── CleanupService.java         (@Scheduled, replaces cleanup.py)
└── exception/         GlobalExceptionHandler (@ControllerAdvice), typed exceptions
```

### Data layer

Flyway `V1__init.sql` creates all 8 tables with the same columns/enums/
constraints as the current Postgres schema (derived from the SQLAlchemy
models, not from Alembic's migration history). JPA entities map 1:1.
`spring.jpa.hibernate.ddl-auto=validate` (Flyway owns schema, Hibernate only
validates).

### REST contract — must not change

Every endpoint path, HTTP method, request/response JSON field name, status
enum value string, and cookie behavior must match the current Python API
exactly, since the frontend is not being touched. `GlobalExceptionHandler`
maps validation/not-found/rate-limit errors to the same HTTP status codes
FastAPI uses today (400/404/429).

### Async pipeline

`@EnableAsync` + a dedicated `ThreadPoolTaskExecutor` bean, `corePoolSize=2`/
`maxPoolSize=4` (matches the current Celery worker's `--concurrency=2` —
this is a low-volume demo, not a scaled worker fleet). `ProjectController`'s
`generate` endpoint sets status QUEUED and calls
`videoGenerationService.processProjectAsync(projectId)`, returning
immediately — same fire-and-forget contract the frontend already expects
from its polling loop. Each pipeline stage updates `progress`/`status` in
the same DB transaction pattern as `pipeline.py`.

### Replicate integration (no official Java SDK — verified via raw HTTP docs)

- Create: `POST https://api.replicate.com/v1/models/kwaivgi/kling-v1.6-standard/predictions`,
  header `Authorization: Bearer <token>`, body `{"input": {"prompt":...,
  "start_image": "data:image/jpeg;base64,...", "duration": 10,
  "negative_prompt": "..."}}`.
- Image input: downscale to ~1024px/JPEG-85 (Thumbnailator, same resize
  utility used for thumbnails) then base64 data-URI — Replicate's
  documented "Option 3", recommended for files under ~1MB, avoids
  implementing their separate Files-upload endpoint.
- Poll: `GET https://api.replicate.com/v1/predictions/{id}` every ~3s until
  `status` is `succeeded`/`failed`/`canceled`, ~10 minute overall timeout.
  On `succeeded`, `output` is a plain string URL (verified against the
  model's schema: output type `uri`, not an array) — download it and hand
  the bytes to `VideoCompositionService`. On `failed`/timeout, throw so the
  existing pipeline error-handling path marks the project FAILED with a
  message — no new error handling needed, same as the Python version.

### Storage & ffmpeg

`StorageService` interface with `LocalStorageService` (java.nio.file,
same `media/projects/{id}/...` layout, `/media` static resource mapping in
`WebConfig`) and `R2StorageService` (AWS SDK v2 S3 client, same key layout).
`VideoCompositionService` shells out to `ffmpeg` via `ProcessBuilder`, same
concat+drawtext filter graph as the Python version.

### Infra

One `Dockerfile` (Maven multi-stage: `maven:3.9-eclipse-temurin-21` build
stage → slim JRE runtime stage with `ffmpeg` installed, mirroring the
current worker image's ffmpeg install). `infrastructure/docker-compose.yml`
shrinks from `postgres, redis, backend, worker, worker-beat, frontend` (6)
to `postgres, backend, frontend` (3).

## Out of scope

- Frontend: no changes.
- Video-generation *behavior*: unchanged from the already-implemented
  Python version (same provider, same prompt template, same cost controls).
- Any new features, auth enforcement, payments, or subscription logic —
  ported as inert schema/dead-code parity only, matching current behavior.
- Preserving Python migration history or live data migration.

## Cutover

Implement the Java app fully, verify it locally (docker-compose), then
delete `backend/` and `worker/` and update root `README.md`'s run
instructions accordingly.
