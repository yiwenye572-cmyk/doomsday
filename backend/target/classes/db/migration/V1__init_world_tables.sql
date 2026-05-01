-- V1: 世界观数据层初始化（pgvector + 核心世界表）

-- 启用 pgvector 扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- 世界观文档（离线工厂原始产出）
CREATE TABLE IF NOT EXISTS world_doc (
    id          BIGSERIAL PRIMARY KEY,
    version     VARCHAR(64)  NOT NULL,
    source      VARCHAR(255) NOT NULL,
    content     TEXT         NOT NULL,
    status      VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- 实体定义（玩家/NPC/怪物）
CREATE TABLE IF NOT EXISTS entity_def (
    entity_id   VARCHAR(128) PRIMARY KEY,
    type        VARCHAR(64)  NOT NULL,
    attrs_json  JSONB        NOT NULL DEFAULT '{}',
    version     VARCHAR(64)  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- 状态机定义（实体生命周期图）
CREATE TABLE IF NOT EXISTS state_machine (
    id               BIGSERIAL PRIMARY KEY,
    entity_type      VARCHAR(64)  NOT NULL,
    state_graph_json JSONB        NOT NULL,
    version          VARCHAR(64)  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Lorebook 条目（世界观背景知识）
CREATE TABLE IF NOT EXISTS lorebook_entry (
    entry_id    VARCHAR(128) PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    body        TEXT         NOT NULL,
    tags_json   JSONB        NOT NULL DEFAULT '[]',
    priority    INT          NOT NULL DEFAULT 50,
    version     VARCHAR(64)  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- 事件卡片（离线预构建，在线检索）
CREATE TABLE IF NOT EXISTS event_card (
    event_id        VARCHAR(128) PRIMARY KEY,
    trigger_json    JSONB   NOT NULL DEFAULT '{}',
    effect_json     JSONB   NOT NULL DEFAULT '{}',
    constraints_json JSONB  NOT NULL DEFAULT '{}',
    rarity          VARCHAR(32) NOT NULL DEFAULT 'COMMON',
    version         VARCHAR(64) NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 规则 DSL
CREATE TABLE IF NOT EXISTS rule_dsl (
    rule_id     VARCHAR(128) PRIMARY KEY,
    scope       VARCHAR(64)  NOT NULL,
    expr        TEXT         NOT NULL,
    priority    INT          NOT NULL DEFAULT 100,
    action      TEXT         NOT NULL,
    version     VARCHAR(64)  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- 向量 Embedding 块（RAG 检索核心）
CREATE TABLE IF NOT EXISTS embedding_chunk (
    chunk_id    BIGSERIAL PRIMARY KEY,
    ref_type    VARCHAR(64)  NOT NULL,
    ref_id      VARCHAR(128) NOT NULL,
    chunk_text  TEXT         NOT NULL,
    embedding   VECTOR(1536),          -- 1536 维对应 text-embedding-3-small
    tags_json   JSONB        NOT NULL DEFAULT '[]',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- 向量索引（cosine 距离，适合语义召回）
CREATE INDEX IF NOT EXISTS idx_embedding_vector
    ON embedding_chunk USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

-- 常用查询索引
CREATE INDEX IF NOT EXISTS idx_lorebook_tags      ON lorebook_entry USING gin(tags_json);
CREATE INDEX IF NOT EXISTS idx_event_card_trigger ON event_card     USING gin(trigger_json);
CREATE INDEX IF NOT EXISTS idx_embedding_ref      ON embedding_chunk(ref_type, ref_id);
