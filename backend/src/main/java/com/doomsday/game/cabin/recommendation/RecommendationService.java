package com.doomsday.game.cabin.recommendation;

import com.doomsday.game.cabin.CabinService;
import com.doomsday.game.cabin.dto.CabinStateResponse;
import com.doomsday.game.cabin.dto.CabinUpdateRequest;
import com.doomsday.game.cabin.dto.CabinUpdateResponse;
import com.doomsday.game.cabin.recommendation.LayoutRecommendationEngine.RecommendationResult;
import com.doomsday.game.common.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 布局推荐 Service
 *
 * 生命周期：
 *   1. GET recommendation → 生成推荐，以 UUID 存入 Redis（TTL 10 分钟）
 *   2. POST accept → 从 Redis 取出 → 调用 CabinService.updateState 应用布局
 *   3. POST reject → 删除 Redis key，记录日志（供后续 ML 训练）
 *
 * 推荐以 Redis String 缓存，key：cabin:rec:{sessionId}:{recId}
 */
@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);
    private static final Duration REC_TTL = Duration.ofMinutes(10);
    private static final String REC_KEY_PREFIX = "cabin:rec:";

    private final LayoutRecommendationEngine engine;
    private final CabinService cabinService;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RecommendationService(LayoutRecommendationEngine engine,
                                 CabinService cabinService,
                                 StringRedisTemplate redis,
                                 ObjectMapper objectMapper) {
        this.engine = engine;
        this.cabinService = cabinService;
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    // ─── Generate ────────────────────────────────────────────────────────

    public LayoutRecommendationResponse generate(String sessionId) {
        // 读取当前小屋状态
        CabinStateResponse state = cabinService.getState(sessionId);

        // 引擎推荐
        RecommendationResult result = engine.recommend(
                state.getStateData(),
                state.getPlayerStamina(),
                state.getTimeOfDay()
        );

        // 生成唯一 ID 并缓存
        String recId = UUID.randomUUID().toString();
        LayoutRecommendationResponse response = new LayoutRecommendationResponse(
                recId,
                sessionId,
                result.items(),
                result.reason(),
                result.confidence(),
                "READY"
        );

        try {
            redis.opsForValue().set(redisKey(sessionId, recId),
                    objectMapper.writeValueAsString(response), REC_TTL);
        } catch (Exception e) {
            log.warn("[RecommendationService] failed to cache recommendation: {}", e.getMessage());
        }

        log.info("[RecommendationService] generated recId={} sessionId={} confidence={:.2f} reason={}",
                recId, sessionId, result.confidence(), result.reason());
        return response;
    }

    // ─── Accept ──────────────────────────────────────────────────────────

    /**
     * 接受推荐：将推荐布局作为 changes 提交给 CabinService（Lua CAS）。
     * 注意：调用方需提供 expectedVersion，用于防并发冲突。
     */
    public CabinUpdateResponse accept(String sessionId, String recId, long expectedVersion) {
        LayoutRecommendationResponse rec = loadOrThrow(sessionId, recId);

        // 将推荐布局转为 change 列表（op=move）
        List<Map<String, Object>> changes = rec.items().stream()
                .map(item -> Map.<String, Object>of(
                        "op", "move",
                        "itemId", item.id(),
                        "payload", Map.of("x", item.x(), "y", item.y())
                ))
                .toList();

        CabinUpdateRequest req = new CabinUpdateRequest();
        req.setIdempotencyKey("rec-accept-" + recId);
        req.setExpectedVersion(expectedVersion);
        req.setChanges(changes);

        CabinUpdateResponse updateResp = cabinService.updateState(sessionId, req);

        if (!updateResp.isConflict()) {
            // 删除缓存（已被消费）
            redis.delete(redisKey(sessionId, recId));
            log.info("[RecommendationService] accepted recId={} sessionId={}", recId, sessionId);
        } else {
            log.warn("[RecommendationService] accept conflict recId={} sessionId={}", recId, sessionId);
        }
        return updateResp;
    }

    // ─── Reject ──────────────────────────────────────────────────────────

    public void reject(String sessionId, String recId) {
        redis.delete(redisKey(sessionId, recId));
        log.info("[RecommendationService] rejected recId={} sessionId={}", recId, sessionId);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────

    private String redisKey(String sessionId, String recId) {
        return REC_KEY_PREFIX + sessionId + ":" + recId;
    }

    private LayoutRecommendationResponse loadOrThrow(String sessionId, String recId) {
        String json = redis.opsForValue().get(redisKey(sessionId, recId));
        if (json == null) {
            throw new ApiException("NOT_FOUND", "Recommendation not found or expired: " + recId);
        }
        try {
            return objectMapper.readValue(json, LayoutRecommendationResponse.class);
        } catch (Exception e) {
            throw new ApiException("INTERNAL", "Failed to parse recommendation: " + e.getMessage());
        }
    }
}
