-- V6: WorldFactory 预编译知识中间表

CREATE TABLE IF NOT EXISTS world_chunk (
    chunk_id        BIGSERIAL PRIMARY KEY,
    world_version   VARCHAR(64) NOT NULL,
    seq_no          INT         NOT NULL,
    chunk_text      TEXT        NOT NULL,
    tags_json       JSONB       NOT NULL DEFAULT '[]',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_world_chunk_version_seq
    ON world_chunk(world_version, seq_no);

CREATE INDEX IF NOT EXISTS idx_world_chunk_tags
    ON world_chunk USING gin(tags_json);

CREATE TABLE IF NOT EXISTS world_entity (
    entity_id       VARCHAR(128) PRIMARY KEY,
    world_version   VARCHAR(64) NOT NULL,
    entity_type     VARCHAR(32) NOT NULL,
    attrs_json      JSONB       NOT NULL DEFAULT '{}',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_world_entity_version
    ON world_entity(world_version);

CREATE TABLE IF NOT EXISTS world_rule (
    rule_id         VARCHAR(128) PRIMARY KEY,
    world_version   VARCHAR(64) NOT NULL,
    scope           VARCHAR(64) NOT NULL,
    expr            TEXT        NOT NULL,
    priority        INT         NOT NULL DEFAULT 100,
    action          TEXT        NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_world_rule_version_scope
    ON world_rule(world_version, scope);
