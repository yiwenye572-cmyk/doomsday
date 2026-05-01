package com.doomsday.game.world;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EventCardRepository extends JpaRepository<EventCard, String> {

    /**
     * 按地点标签召回 Top-K 事件卡（P1：JSON 字段包含匹配）。
     * trigger_json 格式示例：{"location": "old_gas_station", ...}
     */
    @Query(value = """
            SELECT * FROM event_card
            WHERE trigger_json->>'location' = :location
            ORDER BY rarity DESC
            LIMIT :topK
            """, nativeQuery = true)
    List<EventCard> findTopByLocationTag(@Param("location") String location,
                                         @Param("topK") int topK);
}
