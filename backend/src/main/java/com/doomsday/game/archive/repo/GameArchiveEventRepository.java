package com.doomsday.game.archive.repo;

import com.doomsday.game.archive.model.GameArchiveEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GameArchiveEventRepository extends JpaRepository<GameArchiveEvent, Long> {

    @Query(value = """
            SELECT * FROM game_archive_event
            WHERE session_id = :sessionId
              AND (:fromTurn IS NULL OR turn_no >= :fromTurn)
              AND (:toTurn IS NULL OR turn_no <= :toTurn)
            ORDER BY turn_no ASC, id ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<GameArchiveEvent> findEvents(
            @Param("sessionId") String sessionId,
            @Param("fromTurn") Integer fromTurn,
            @Param("toTurn") Integer toTurn,
            @Param("limit") int limit
    );
}
