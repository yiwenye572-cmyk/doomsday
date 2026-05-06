package com.doomsday.game.diary.repo;

import com.doomsday.game.diary.model.DiaryEntryL2;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GameDiaryL2Repository extends JpaRepository<DiaryEntryL2, Long> {

    @Query(value = """
            SELECT * FROM game_diary_l2
            WHERE session_id = :sessionId
              AND (:fromTurn IS NULL OR to_turn >= :fromTurn)
              AND (:toTurn IS NULL OR from_turn <= :toTurn)
            ORDER BY to_turn DESC, id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<DiaryEntryL2> findEntries(
            @Param("sessionId") String sessionId,
            @Param("fromTurn") Integer fromTurn,
            @Param("toTurn") Integer toTurn,
            @Param("limit") int limit
    );
}
