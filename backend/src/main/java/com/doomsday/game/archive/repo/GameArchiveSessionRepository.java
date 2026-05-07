package com.doomsday.game.archive.repo;

import com.doomsday.game.archive.model.GameArchiveSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameArchiveSessionRepository extends JpaRepository<GameArchiveSession, String> {
}
