package com.doomsday.game.diary.repo;

import com.doomsday.game.diary.model.DiaryEntryL1;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GameDiaryL1Repository extends JpaRepository<DiaryEntryL1, Long> {

    @Query(value = """
            SELECT * FROM game_diary_l1
            WHERE session_id = :sessionId
              AND (:fromTurn IS NULL OR to_turn >= :fromTurn)
              AND (:toTurn IS NULL OR from_turn <= :toTurn)
            ORDER BY to_turn DESC, id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<DiaryEntryL1> findEntries(
            @Param("sessionId") String sessionId,
            @Param("fromTurn") Integer fromTurn,
            @Param("toTurn") Integer toTurn,
            @Param("limit") int limit
    );

    @Query(value = """
            SELECT COALESCE(MAX(to_turn), 0)
            FROM game_diary_l1
            WHERE session_id = :sessionId
            """, nativeQuery = true)
    int findLastSummarizedTurn(@Param("sessionId") String sessionId);
}
