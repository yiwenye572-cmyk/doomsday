package com.doomsday.game.cabin;

import com.doomsday.game.cabin.ItemStoryEntity.Status;
import com.doomsday.game.cabin.dto.ItemStoryResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * 物品叙事 Service
 *
 * 幂等流程：
 *   1. 查 Redis 缓存（key: cabin:story:{sessionId}:{itemId}）
 *      → 命中 → 直接返回 DONE
 *   2. 查 DB（item_story 表）
 *      → DONE    → 写 Redis 缓存后返回 DONE
 *      → PENDING/RUNNING → 返回 202 状态
 *      → FAILED  → 重置为 PENDING + 重新触发
 *      → 无记录  → 创建 PENDING → 置 RUNNING → 触发 Worker
 *   3. 首次触发返回 202 + {taskId, status:PENDING}
 *      轮询时（已有任务）按实际状态返回
 */
@Service
public class ItemStoryService {

    private static final Logger log = LoggerFactory.getLogger(ItemStoryService.class);
    private static final Duration STORY_CACHE_TTL = Duration.ofDays(7);

    private final ItemStoryRepository repository;
    private final ItemStoryWorker worker;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public ItemStoryService(ItemStoryRepository repository,
                            ItemStoryWorker worker,
                            StringRedisTemplate redis,
                            ObjectMapper objectMapper) {
        this.repository = repository;
        this.worker = worker;
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    /**
     * 查询物品叙事。
     * 首次调用：入库 + 异步触发，返回 (202, PENDING)
     * 轮询调用：按 DB 状态返回；DONE 命中缓存后返回 (200, DONE)
     *
     * @param sessionId   游戏会话 ID
     * @param itemId      物品 ID
     * @param itemType    物品类型（首次触发时使用）
     * @param itemMetadata 物品元数据 JSON（可选，首次触发时使用）
     * @return (isAccepted=true → HTTP 202, false → HTTP 200)
     */
    @Transactional
    public StoryResult getOrTrigger(String sessionId, String itemId,
                                    String itemType, String itemMetadata) {
        // ── Step 1: Redis 快路径 ─────────────────────────────────────────
        String cacheKey = cacheKey(sessionId, itemId);
        String cached = redis.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("[ItemStoryService] cache hit key={}", cacheKey);
            return StoryResult.done(buildDoneResponse(sessionId, itemId, null, cached, null));
        }

        // ── Step 2: 查 DB ────────────────────────────────────────────────
        ItemStoryEntity entity = repository
                .findBySessionIdAndItemId(sessionId, itemId)
                .orElse(null);

        if (entity != null) {
            switch (entity.getStatus()) {
                case DONE -> {
                    // 补写缓存
                    redis.opsForValue().set(cacheKey, entity.getStoryText(), STORY_CACHE_TTL);
                    return StoryResult.done(buildDoneResponse(sessionId, itemId, entity.getId(),
                            entity.getStoryText(), entity.getRagCitations()));
                }
                case PENDING, RUNNING -> {
                    return StoryResult.accepted(buildPendingResponse(entity));
                }
                case FAILED -> {
                    // 重试：重置状态 → 重新触发
                    log.info("[ItemStoryService] retry failed task sessionId={} itemId={}", sessionId, itemId);
                    entity.setStatus(Status.RUNNING);
                    entity.setErrorMessage(null);
                    entity.setStoryText(null);
                    repository.save(entity);
                    worker.generateStory(entity.getId());
                    return StoryResult.accepted(buildPendingResponse(entity));
                }
            }
        }

        // ── Step 3: 首次触发 ─────────────────────────────────────────────
        ItemStoryEntity newEntity = new ItemStoryEntity();
        newEntity.setSessionId(sessionId);
        newEntity.setItemId(itemId);
        newEntity.setItemType(itemType);
        newEntity.setItemMetadata(itemMetadata);
        newEntity.setStatus(Status.RUNNING);   // 直接置 RUNNING，避免 Worker 启动前被重复触发
        ItemStoryEntity saved = repository.save(newEntity);

        log.info("[ItemStoryService] new task created taskId={} sessionId={} itemId={}", saved.getId(), sessionId, itemId);
        worker.generateStory(saved.getId());

        return StoryResult.accepted(buildPendingResponse(saved));
    }

    // ─── Private Helpers ──────────────────────────────────────────────────

    private String cacheKey(String sessionId, String itemId) {
        return "cabin:story:" + sessionId + ":" + itemId;
    }

    private ItemStoryResponse buildDoneResponse(String sessionId, String itemId,
                                                 Long taskId, String storyText,
                                                 String ragCitations) {
        return new ItemStoryResponse(
                taskId,
                sessionId,
                itemId,
                "DONE",
                storyText,
                null,
                Instant.now()
        );
    }

    private ItemStoryResponse buildPendingResponse(ItemStoryEntity entity) {
        return new ItemStoryResponse(
                entity.getId(),
                entity.getSessionId(),
                entity.getItemId(),
                entity.getStatus().name(),
                null,
                entity.getStatus() == Status.FAILED ? entity.getErrorMessage() : null,
                null
        );
    }

    // ─── Value Object ─────────────────────────────────────────────────────

    public record StoryResult(boolean accepted, ItemStoryResponse response) {
        public static StoryResult done(ItemStoryResponse r)     { return new StoryResult(false, r); }
        public static StoryResult accepted(ItemStoryResponse r) { return new StoryResult(true, r); }
    }
}
