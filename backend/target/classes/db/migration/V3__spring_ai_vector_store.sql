-- V3: Spring AI PgVector Store 兼容表
-- 供 Spring AI VectorStore（RAG 召回）使用
-- 独立于 embedding_chunk（保留备用），由 Spring AI 负责 CRUD

-- 启用 vector 扩展（V1 已启用，此处幂等）
CREATE EXTENSION IF NOT EXISTS vector;

-- Spring AI PgVectorStore 标准 schema
CREATE TABLE IF NOT EXISTS vector_store (
    id          UUID        DEFAULT gen_random_uuid() PRIMARY KEY,
    content     TEXT        NOT NULL,
    metadata    JSONB       NOT NULL DEFAULT '{}',
    embedding   VECTOR(1536)
);

-- ivfflat cosine 索引（与 V1 embedding_chunk 保持一致策略）
CREATE INDEX IF NOT EXISTS idx_vector_store_embedding
    ON vector_store USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

-- 元数据标签索引（支持按 source/location 过滤）
CREATE INDEX IF NOT EXISTS idx_vector_store_metadata
    ON vector_store USING gin (metadata);
