-- V9: 持久化存档与小说化回放事件

CREATE TABLE IF NOT EXISTS game_archive_session (
    session_id              VARCHAR(96) PRIMARY KEY,
    world_version           VARCHAR(64),
    difficulty              VARCHAR(24) NOT NULL,
    latest_turn             INT NOT NULL DEFAULT 0,
    latest_version          BIGINT NOT NULL DEFAULT 1,
    day_index               INT NOT NULL DEFAULT 1,
    turn_in_day             INT NOT NULL DEFAULT 1,
    turns_per_day_target    INT NOT NULL DEFAULT 4,
    time_phase              VARCHAR(24) NOT NULL DEFAULT 'MIDNIGHT',
    state_json              TEXT NOT NULL,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_archive_session_updated
    ON game_archive_session(updated_at DESC);

CREATE TABLE IF NOT EXISTS game_archive_event (
    id                  BIGSERIAL PRIMARY KEY,
    session_id          VARCHAR(96) NOT NULL,
    turn_no             INT NOT NULL,
    day_index           INT NOT NULL DEFAULT 1,
    time_phase          VARCHAR(24) NOT NULL DEFAULT 'MIDNIGHT',
    action_type         VARCHAR(32) NOT NULL,
    narrative           TEXT NOT NULL,
    state_delta_json    TEXT NOT NULL DEFAULT '{}',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_archive_event_session_turn
    ON game_archive_event(session_id, turn_no ASC, id ASC);

CREATE INDEX IF NOT EXISTS idx_archive_event_created
    ON game_archive_event(created_at DESC);
