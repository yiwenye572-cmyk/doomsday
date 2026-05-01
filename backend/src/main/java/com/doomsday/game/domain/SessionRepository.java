package com.doomsday.game.domain;

import com.doomsday.game.api.SubmitTurnResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * Redis 会话仓库。
 * Key 规则：
 *   - 会话：  game:session:{sessionId}       TTL 24h
 *   - 幂等键：game:idem:{sessionId}:{idemKey}  TTL 24h
 *   - L0记忆：game:memory:{sessionId}        列表保留最近 N 条
 */
@Repository
public class SessionRepository {

    private static final String SESSION_PREFIX = "game:session:";
    private static final String IDEM_PREFIX = "game:idem:";
    private static final String MEMORY_PREFIX = "game:memory:";
    private static final String EPISODIC_PREFIX = "game:memory:l1:";
    private static final Duration SESSION_TTL = Duration.ofHours(24);
    private static final int MEMORY_WINDOW = 6;
    private static final int EPISODIC_WINDOW = 20;

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

    // ===== L0 Rolling Memory =====

    public void appendTurnMemory(String sessionId, TurnMemory memory) {
        try {
            String key = MEMORY_PREFIX + sessionId;
            String json = objectMapper.writeValueAsString(memory);
            redis.opsForList().rightPush(key, json);
            redis.opsForList().trim(key, -MEMORY_WINDOW, -1);
            redis.expire(key, SESSION_TTL);
        } catch (Exception e) {
            throw new RuntimeException("failed to append rolling memory: " + sessionId, e);
        }
    }

    public List<TurnMemory> findRecentTurnMemories(String sessionId, int topN) {
        String key = MEMORY_PREFIX + sessionId;
        long n = Math.max(1, topN);
        List<String> raw = redis.opsForList().range(key, -n, -1);
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }

        List<TurnMemory> parsed = new ArrayList<>(raw.size());
        for (String json : raw) {
            try {
                parsed.add(objectMapper.readValue(json, TurnMemory.class));
            } catch (Exception ignored) {
                // 单条损坏不影响主流程
            }
        }
        return parsed;
    }

    // ===== L1 Episodic Summary =====

    public void appendEpisodicSummary(String sessionId, String summary) {
        if (summary == null || summary.isBlank()) {
            return;
        }
        String key = EPISODIC_PREFIX + sessionId;
        redis.opsForList().rightPush(key, summary);
        redis.opsForList().trim(key, -EPISODIC_WINDOW, -1);
        redis.expire(key, SESSION_TTL);
    }

    public List<String> findRecentEpisodicSummaries(String sessionId, int topN) {
        String key = EPISODIC_PREFIX + sessionId;
        long n = Math.max(1, topN);
        List<String> rows = redis.opsForList().range(key, -n, -1);
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return rows.stream().filter(s -> s != null && !s.isBlank()).toList();
    }
}
