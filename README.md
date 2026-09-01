# KidsStory

A service that turns a photo and a story into a short AI-animated children's video.

## User Flow

1. **Upload a photo** — of a child, family, pet, drawing, or toy (1 free, 2-3 on Basic, 5+ on Premium)
2. **Write a story** — a sentence or two is enough for AI to expand into a full story; leave it blank and AI generates one from the photo alone
3. **Choose options** — child's age, video length (30s/1min/3min), style (3D/storybook/watercolor/cartoon), voice, language
4. **AI generation pipeline** — photo analysis → story generation/expansion → scene splitting → scene rendering → narration →
   music/sound effects → final MP4 composition → user notification

## Folder Structure

```
frontend/        Next.js 15 + TypeScript + Tailwind CSS + shadcn/ui + React Query
backend/         Spring Boot 3 (Java 21) + Spring Data JPA + Flyway + PostgreSQL
                 The REST API and the (async) AI video-generation pipeline are combined into a single service.
infrastructure/  docker-compose and environment configuration
```

## Running Locally

### 1) Full stack via Docker Compose (recommended)

```bash
cp backend/.env.example backend/.env
cd infrastructure
docker compose up --build
```

- Frontend: http://localhost:3000
- Backend API: http://localhost:8000

### 2) Running services individually

**Frontend**

```bash
cd frontend
npm install
cp .env.local.example .env.local
npm run dev
```

**Backend** (requires Java 21, Maven, PostgreSQL)

```bash
cd backend
cp .env.example .env
./mvnw spring-boot:run
```

Flyway creates the schema automatically on startup — no separate migration command needed.

## AI Video Generation

`SceneRenderingService` and `VideoCompositionService` are wired to a real image-to-video
generation API. The rest of the pipeline stages (photo analysis, story generation, narration,
music mixing) remain free, local stubs — for demo purposes, wiring up just the one real
video-generation call is enough.

**Flow**: the user's uploaded photo and (optional) story text → the backend creates a project
and queues it on the async pipeline (`@Async`, running inside the backend process itself with
no separate broker) → the photo plus a motion prompt are sent to
[Replicate](https://replicate.com)'s `kwaivgi/kling-v1.6-standard` model to generate a short
video clip → ffmpeg burns in a watermark and composes the final MP4 → uploaded to storage
(R2/local) → the frontend polls every 3 seconds and plays the video once it's done.

**Provider**: Replicate's Kling v1.6 Standard. There's no official Java SDK, so the backend
calls the documented HTTP API directly (create prediction → poll → download result). Pricing is
$0.05/sec, so one free-tier 10-second video costs about $0.50 — cheap enough for 1-2 demo videos.

**Required environment variables** (`backend/.env`):

```
VIDEO_GEN_API_KEY=<your Replicate API token>
VIDEO_GEN_MODEL=kwaivgi/kling-v1.6-standard   # default, swap for a different model if desired
```

**Generating a demo video locally**:

1. Get a token at [replicate.com/account/api-tokens](https://replicate.com/account/api-tokens)
   and set it as `VIDEO_GEN_API_KEY` in `backend/.env`.
2. Start the full stack with `docker compose up --build` (or run the backend individually).
3. At http://localhost:3000, upload one photo → optionally type a sentence or two for the story
   → click "Create My Story".
4. Progress updates automatically, and the video plays on the page once it's done. The generated
   video is saved to R2 (if `STORAGE_BACKEND=r2`) or to
   `media/projects/{project_id}/final.mp4` (if `local`), so it can be reused for the demo
   without regenerating.

## Notes

- The remaining pipeline stages (photo analysis, story generation, scene splitting, narration,
  music mixing) are stub implementations that can be swapped for real AI APIs
  (`com.kidsstory.service.*Service` — see each class's Javadoc).
- Auth (JWT) has been ported to `AuthController` but is not registered on the router — this MVP
  uses cookie+IP-based anonymous identification only, no accounts
  (`AnonIdentityArgumentResolver`).
- The billing/subscription/credit entities (`User`, `Payment`, `Subscription`,
  `CreditTransaction`) have been ported for schema parity but aren't currently used by any
  service logic.
