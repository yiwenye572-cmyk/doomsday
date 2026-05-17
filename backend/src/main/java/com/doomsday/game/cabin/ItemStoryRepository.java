package com.doomsday.game.cabin;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ItemStoryRepository extends JpaRepository<ItemStoryEntity, Long> {
    Optional<ItemStoryEntity> findBySessionIdAndItemId(String sessionId, String itemId);
}
