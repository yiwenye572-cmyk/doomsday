-- V4: 修正 embedding 向量维度为 1024
-- text-embedding-v3（DashScope）默认输出 1024 维
-- V1/V3 中的 VECTOR(1536) 与模型实际输出不符，此处 ALTER 修正

-- ===== vector_store（Spring AI VectorStore / RAG） =====
-- 先清空旧 1536 维向量数据（pgvector 不支持不同维度间的隐式 CAST）
TRUNCATE TABLE vector_store;

-- 删除旧 ivfflat 索引（需先删再改列类型）
DROP INDEX IF EXISTS idx_vector_store_embedding;

-- ALTER 列类型为 1024 维
ALTER TABLE vector_store
    ALTER COLUMN embedding TYPE VECTOR(1024);

-- 重建 ivfflat cosine 索引
CREATE INDEX IF NOT EXISTS idx_vector_store_embedding
    ON vector_store USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

-- ===== embedding_chunk（备用 RAG 表） =====
TRUNCATE TABLE embedding_chunk;

DROP INDEX IF EXISTS idx_embedding_vector;

ALTER TABLE embedding_chunk
    ALTER COLUMN embedding TYPE VECTOR(1024);

CREATE INDEX IF NOT EXISTS idx_embedding_vector
    ON embedding_chunk USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);
