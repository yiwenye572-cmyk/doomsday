package com.doomsday.game.domain;

import com.doomsday.game.api.SubmitTurnResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * Redis 会话仓库。
 * Key 规则：
 *   - 会话：  game:session:{sessionId}       TTL 24h
 *   - 幂等键：game:idem:{sessionId}:{idemKey}  TTL 24h
 */
@Repository
public class SessionRepository {

    private static final String SESSION_PREFIX = "game:session:";
    private static final String IDEM_PREFIX = "game:idem:";
    private static final Duration SESSION_TTL = Duration.ofHours(24);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public SessionRepository(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    // ===== 会话 CRUD =====

    public void save(GameSession session) {
        try {
            String key = SESSION_PREFIX + session.getSessionId();
            String json = objectMapper.writeValueAsString(session);
            redis.opsForValue().set(key, json, SESSION_TTL);
        } catch (Exception e) {
            throw new RuntimeException("failed to save session: " + session.getSessionId(), e);
        }
    }

    public GameSession findById(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        String json = redis.opsForValue().get(key);
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, GameSession.class);
        } catch (Exception e) {
            throw new RuntimeException("failed to deserialize session: " + sessionId, e);
        }
    }

    // ===== 幂等键 =====

    public void saveIdempotent(String idemKey, SubmitTurnResponse response) {
        try {
            String key = IDEM_PREFIX + idemKey;
            String json = objectMapper.writeValueAsString(response);
            redis.opsForValue().set(key, json, SESSION_TTL);
        } catch (Exception e) {
            throw new RuntimeException("failed to save idempotency entry", e);
        }
    }

    public SubmitTurnResponse findIdempotent(String idemKey) {
        String key = IDEM_PREFIX + idemKey;
        String json = redis.opsForValue().get(key);
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, SubmitTurnResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("failed to deserialize idempotency entry", e);
        }
    }
}
