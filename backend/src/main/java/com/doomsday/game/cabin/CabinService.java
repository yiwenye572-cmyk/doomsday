package com.doomsday.game.cabin;

import com.doomsday.game.cabin.dto.*;
import com.doomsday.game.common.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 小屋业务服务
 *
 * 并发控制策略：
 *   1. 写操作通过 Redis Lua 脚本实现原子 CAS（Compare-And-Swap）
 *   2. Redis 为热态（TTL 24h）；成功写 Redis 后异步（@Async）持久化到 PostgreSQL
 *   3. 409 冲突时返回最新 state，前端自行合并或提示刷新
 */
@Service
public class CabinService {

    private static final String CABIN_KEY_PREFIX = "cabin:state:";
    private static final String CABIN_VER_PREFIX  = "cabin:ver:";
    private static final Duration CABIN_TTL = Duration.ofHours(24);

    /**
     * Lua CAS 脚本：
     *   KEYS[1] = version key, KEYS[2] = state key
     *   ARGV[1] = expectedVersion, ARGV[2] = newVersion, ARGV[3] = newStateJson, ARGV[4] = ttlSeconds
     * 返回 1 = 成功，0 = 冲突（版本不匹配）
     */
    private static final String LUA_CAS = """
        local cur = redis.call('GET', KEYS[1])
        if cur == false then cur = '0' end
        if tostring(cur) ~= tostring(ARGV[1]) then
            return 0
        end
        redis.call('SET', KEYS[1], ARGV[2], 'EX', ARGV[4])
        redis.call('SET', KEYS[2], ARGV[3], 'EX', ARGV[4])
        return 1
        """;

    private final CabinRepository cabinRepository;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public CabinService(CabinRepository cabinRepository, StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.cabinRepository = cabinRepository;
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    // ─── GET ───────────────────────────────────────────────────────────────

    public CabinStateResponse getState(String sessionId) {
        CabinEntity entity = loadOrThrow(sessionId);
        return toResponse(entity);
    }

    // ─── UPDATE（Redis Lua CAS） ───────────────────────────────────────────

    public CabinUpdateResponse updateState(String sessionId, CabinUpdateRequest req) {
        CabinEntity entity = loadOrThrow(sessionId);

        String verKey   = CABIN_VER_PREFIX + sessionId;
        String stateKey = CABIN_KEY_PREFIX + sessionId;
        long expectedVer = req.getExpectedVersion();
        long newVer      = expectedVer + 1;

        // 构建新状态 JSON（简单追加变更到 stateData）
        String newStateJson = applyChanges(entity.getStateData(), req.getChanges());

        DefaultRedisScript<Long> script = new DefaultRedisScript<>(LUA_CAS, Long.class);
        Long result = redis.execute(
                script,
                List.of(verKey, stateKey),
                String.valueOf(expectedVer),
                String.valueOf(newVer),
                newStateJson,
                String.valueOf(CABIN_TTL.toSeconds())
        );

        if (result == null || result == 0L) {
            // 版本冲突 → 返回最新状态
            String currentStateJson = redis.opsForValue().get(stateKey);
            String latestState = currentStateJson != null ? currentStateJson : entity.getStateData();
            return CabinUpdateResponse.conflict(sessionId, entity.getVersion(), latestState);
        }

        // 异步持久化到 PostgreSQL
        entity.setStateData(newStateJson);
        entity.setVersion(newVer);
        cabinRepository.save(entity);

        return new CabinUpdateResponse(sessionId, newVer, newStateJson);
    }

    // ─── TAKE（出门携带） ─────────────────────────────────────────────────

    @Transactional
    public CabinStateResponse takeItems(String sessionId, CabinTakeRequest req) {
        CabinEntity entity = loadOrThrow(sessionId);
        // 标记物品为 carried（从 stateData 中移除 / 追加 carried 标记）
        String updatedState = markItemsCarried(entity.getStateData(), req.getItemIds());
        entity.setStateData(updatedState);
        cabinRepository.save(entity);
        return toResponse(entity);
    }

    // ─── RETURN（归来） ──────────────────────────────────────────────────

    @Transactional
    public CabinStateResponse returnItems(String sessionId, CabinReturnRequest req) {
        CabinEntity entity = loadOrThrow(sessionId);
        // 将新物品追加到待整理区（pendingTray）
        String updatedState = appendToPendingTray(entity.getStateData(), req.getFoundItems());
        entity.setStateData(updatedState);
        cabinRepository.save(entity);
        // TODO: 异步触发物品故事生成（Week 4 AI 模块）
        return toResponse(entity);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────

    private CabinEntity loadOrThrow(String sessionId) {
        return cabinRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Cabin state not found for session: " + sessionId));
    }

    private CabinStateResponse toResponse(CabinEntity e) {
        return new CabinStateResponse(e.getSessionId(), e.getVersion(), e.getStateData(),
                e.getPlayerStamina(), e.getTimeOfDay());
    }

    /**
     * 将 changes 列表应用到当前 stateData（JSON 字符串）。
     * 当前实现：直接将变更列表序列化并附加（Week 2 简化版，后续可做精确 JSON Patch）。
     */
    private String applyChanges(String currentState, List<Map<String, Object>> changes) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> stateMap = currentState != null
                    ? objectMapper.readValue(currentState, Map.class)
                    : new java.util.HashMap<>();
            stateMap.put("_lastChanges", changes);
            return objectMapper.writeValueAsString(stateMap);
        } catch (Exception e) {
            return currentState != null ? currentState : "{}";
        }
    }

    private String markItemsCarried(String stateData, List<String> itemIds) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> stateMap = stateData != null
                    ? objectMapper.readValue(stateData, Map.class)
                    : new java.util.HashMap<>();
            stateMap.put("carriedItems", itemIds);
            return objectMapper.writeValueAsString(stateMap);
        } catch (Exception e) {
            return stateData != null ? stateData : "{}";
        }
    }

    private String appendToPendingTray(String stateData, List<Map<String, Object>> foundItems) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> stateMap = stateData != null
                    ? objectMapper.readValue(stateData, Map.class)
                    : new java.util.HashMap<>();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> tray = (List<Map<String, Object>>) stateMap.getOrDefault("pendingTray", new java.util.ArrayList<>());
            if (foundItems != null) tray.addAll(foundItems);
            stateMap.put("pendingTray", tray);
            return objectMapper.writeValueAsString(stateMap);
        } catch (Exception e) {
            return stateData != null ? stateData : "{}";
        }
    }
}
