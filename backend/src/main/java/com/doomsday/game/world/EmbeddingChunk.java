package com.doomsday.game.world;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "embedding_chunk")
public class EmbeddingChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chunk_id")
    private Long chunkId;

    @Column(name = "ref_type", nullable = false)
    private String refType;

    @Column(name = "ref_id", nullable = false)
    private String refId;

    @Column(name = "chunk_text", nullable = false, columnDefinition = "TEXT")
    private String chunkText;

    /** 向量列通过 SQL/pgvector 原生操作管理，此处仅作数据映射占位 */
    @Column(name = "tags_json", columnDefinition = "jsonb")
    private String tagsJson;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public EmbeddingChunk() {}

    public Long getChunkId() { return chunkId; }
    public String getRefType() { return refType; }
    public String getRefId() { return refId; }
    public String getChunkText() { return chunkText; }
    public String getTagsJson() { return tagsJson; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
