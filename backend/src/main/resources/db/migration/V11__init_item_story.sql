-- V11: 物品叙事任务表（item_story）
-- 状态机：PENDING → RUNNING → DONE / FAILED
-- 生成结果缓存在此表，避免重复调 LLM

CREATE TABLE IF NOT EXISTS item_story (
    id              BIGSERIAL    PRIMARY KEY,
    session_id      VARCHAR(128) NOT NULL,
    item_id         VARCHAR(128) NOT NULL,
    item_type       VARCHAR(64),
    item_metadata   TEXT,                         -- 物品元数据 JSON（供 Prompt 组装）
    status          VARCHAR(16)  NOT NULL DEFAULT 'PENDING', -- PENDING|RUNNING|DONE|FAILED
    story_text      TEXT,                         -- LLM 生成的叙事结果
    rag_citations   TEXT,                         -- RAG 引用片段（JSON 数组）
    error_message   VARCHAR(512),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (session_id, item_id)
);

CREATE INDEX IF NOT EXISTS idx_item_story_session_item ON item_story(session_id, item_id);
CREATE INDEX IF NOT EXISTS idx_item_story_status       ON item_story(status);
