package com.doomsday.game.world;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LorebookEntryRepository extends JpaRepository<LorebookEntry, String> {

    /**
     * 按关键词全文搜索 body 字段，返回 Top-K 优先级最高的条目。
     * P1：简单 ILIKE 搜索；后续替换为 pgvector 向量召回。
     */
    @Query(value = """
            SELECT * FROM lorebook_entry
            WHERE body ILIKE CONCAT('%', :keyword, '%')
              AND version = :worldVersion
            ORDER BY priority DESC
            LIMIT :topK
            """, nativeQuery = true)
    List<LorebookEntry> findTopByKeywordAndVersion(@Param("keyword") String keyword,
                                                   @Param("worldVersion") String worldVersion,
                                                   @Param("topK") int topK);

    @Query(value = """
            SELECT COUNT(1) FROM lorebook_entry WHERE version = :worldVersion
            """, nativeQuery = true)
    long countByWorldVersion(@Param("worldVersion") String worldVersion);
}
