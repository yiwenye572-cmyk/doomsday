package com.doomsday.game.cabin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CabinRepository extends JpaRepository<CabinEntity, Long> {

    Optional<CabinEntity> findBySessionId(String sessionId);
}