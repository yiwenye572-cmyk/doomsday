-- V7: 游戏日记系统（L1/L2）

CREATE TABLE IF NOT EXISTS game_diary_l1 (
    id              BIGSERIAL PRIMARY KEY,
    session_id      VARCHAR(96)  NOT NULL,
    world_version   VARCHAR(64),
    from_turn       INT          NOT NULL,
    to_turn         INT          NOT NULL,
    summary         TEXT         NOT NULL,
    tags_json       JSONB        NOT NULL DEFAULT '[]',
    source          VARCHAR(24)  NOT NULL DEFAULT 'AUTO',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_diary_l1_turn_range CHECK (to_turn >= from_turn)
);

CREATE INDEX IF NOT EXISTS idx_diary_l1_session_turn
    ON game_diary_l1(session_id, to_turn DESC);

CREATE INDEX IF NOT EXISTS idx_diary_l1_session_created
    ON game_diary_l1(session_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_diary_l1_tags_gin
    ON game_diary_l1 USING gin(tags_json);

CREATE TABLE IF NOT EXISTS game_diary_l2 (
    id              BIGSERIAL PRIMARY KEY,
    session_id      VARCHAR(96)  NOT NULL,
    world_version   VARCHAR(64),
    from_turn       INT          NOT NULL,
    to_turn         INT          NOT NULL,
    topic           VARCHAR(64)  NOT NULL,
    summary         TEXT         NOT NULL,
    key_facts_json  JSONB        NOT NULL DEFAULT '[]',
    source          VARCHAR(24)  NOT NULL DEFAULT 'AUTO',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_diary_l2_turn_range CHECK (to_turn >= from_turn)
);

CREATE INDEX IF NOT EXISTS idx_diary_l2_session_turn
    ON game_diary_l2(session_id, to_turn DESC);

CREATE INDEX IF NOT EXISTS idx_diary_l2_topic
    ON game_diary_l2(session_id, topic, created_at DESC);
