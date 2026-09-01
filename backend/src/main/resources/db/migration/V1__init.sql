-- Fresh schema for the Java rewrite (no need to preserve the Python
-- version's Alembic migration history - this is a dev database, not a
-- live production system). Enum columns store the Java enum's constant
-- name (e.g. 'AGE_3_5'), not its JSON value - only the REST API's JSON
-- representation needs to match the frontend, not the DB storage format.

CREATE TABLE users (
    id                UUID PRIMARY KEY,
    email             VARCHAR(255) NOT NULL UNIQUE,
    hashed_password   VARCHAR(255) NOT NULL,
    display_name      VARCHAR(100),
    created_at        TIMESTAMPTZ NOT NULL,
    updated_at        TIMESTAMPTZ NOT NULL
);

CREATE TABLE projects (
    id                UUID PRIMARY KEY,
    user_id           UUID REFERENCES users(id) ON DELETE CASCADE,
    anon_id           VARCHAR(64),
    client_ip         VARCHAR(64),
    title             VARCHAR(200) NOT NULL,
    story_prompt      TEXT,
    generated_story   TEXT,
    plan              VARCHAR(20) NOT NULL,
    status            VARCHAR(30) NOT NULL,
    progress          INTEGER NOT NULL,
    error_message     TEXT,
    age_group         VARCHAR(10) NOT NULL,
    video_length      INTEGER NOT NULL,
    style             VARCHAR(20) NOT NULL,
    voice             VARCHAR(20) NOT NULL,
    language          VARCHAR(5) NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL,
    updated_at        TIMESTAMPTZ NOT NULL,
    expires_at        TIMESTAMPTZ
);
CREATE INDEX idx_projects_user_id ON projects (user_id);
CREATE INDEX idx_projects_anon_id ON projects (anon_id);
CREATE INDEX idx_projects_client_ip ON projects (client_ip);
CREATE INDEX idx_projects_expires_at ON projects (expires_at);
CREATE INDEX idx_projects_created_at ON projects (created_at);

CREATE TABLE project_images (
    id                UUID PRIMARY KEY,
    project_id        UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    url               VARCHAR(500) NOT NULL,
    thumbnail_url     VARCHAR(500) NOT NULL,
    display_order     INTEGER NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_project_images_project_id ON project_images (project_id);

CREATE TABLE scenes (
    id                    UUID PRIMARY KEY,
    project_id            UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    display_order         INTEGER NOT NULL,
    narration             TEXT NOT NULL,
    visual_description    TEXT,
    image_url             VARCHAR(500),
    video_url             VARCHAR(500),
    narration_audio_url   VARCHAR(500),
    mixed_audio_url       VARCHAR(500),
    created_at            TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_scenes_project_id ON scenes (project_id);

CREATE TABLE videos (
    id                  UUID PRIMARY KEY,
    project_id          UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    status              VARCHAR(20) NOT NULL,
    url                 VARCHAR(500),
    thumbnail_url       VARCHAR(500),
    duration_seconds    INTEGER,
    error_message       TEXT,
    created_at          TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_videos_project_id ON videos (project_id);

CREATE TABLE credits (
    id                UUID PRIMARY KEY,
    user_id           UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    project_id        UUID REFERENCES projects(id) ON DELETE SET NULL,
    amount            INTEGER NOT NULL,
    balance_after     INTEGER NOT NULL,
    reason            VARCHAR(30) NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_credits_user_id ON credits (user_id);

CREATE TABLE subscriptions (
    id                          UUID PRIMARY KEY,
    user_id                     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    plan                        VARCHAR(20) NOT NULL,
    status                      VARCHAR(20) NOT NULL,
    provider                    VARCHAR(50),
    provider_subscription_id    VARCHAR(255),
    current_period_start        TIMESTAMPTZ,
    current_period_end          TIMESTAMPTZ,
    canceled_at                 TIMESTAMPTZ,
    created_at                  TIMESTAMPTZ NOT NULL,
    updated_at                  TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_subscriptions_user_id ON subscriptions (user_id);

CREATE TABLE payments (
    id                    UUID PRIMARY KEY,
    user_id               UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subscription_id       UUID REFERENCES subscriptions(id) ON DELETE SET NULL,
    amount                INTEGER NOT NULL,
    currency              VARCHAR(3) NOT NULL,
    status                VARCHAR(20) NOT NULL,
    provider              VARCHAR(20) NOT NULL,
    provider_payment_id   VARCHAR(255),
    description           VARCHAR(255),
    created_at            TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_payments_user_id ON payments (user_id);
