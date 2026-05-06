-- V5: WorldFactory 离线任务表

CREATE TABLE IF NOT EXISTS world_factory_job (
    job_id         VARCHAR(64) PRIMARY KEY,
    world_version  VARCHAR(64)  NOT NULL,
    source_type    VARCHAR(32)  NOT NULL,
    status         VARCHAR(16)  NOT NULL,
    progress       INT          NOT NULL DEFAULT 0,
    stage          VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    force_rebuild  BOOLEAN      NOT NULL DEFAULT FALSE,
    raw_content    TEXT         NOT NULL,
    error_message  TEXT,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_world_factory_job_world_version
    ON world_factory_job(world_version);

CREATE INDEX IF NOT EXISTS idx_world_factory_job_status
    ON world_factory_job(status);
