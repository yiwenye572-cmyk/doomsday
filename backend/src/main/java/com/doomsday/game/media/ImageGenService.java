package com.doomsday.game.media;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 通义万象 wanx-v1 图片生成 Service
 *
 * 流程：
 *  1. submitTask(prompt, type) → 调 DashScope 异步任务 API，返回 taskId
 *  2. queryTask(taskId) → 轮询任务状态直至 SUCCEEDED/FAILED
 *
 * 缓存策略：相同 prompt 的成功结果缓存 7 天（key: image:gen:{sha256(prompt)}）
 *
 * DashScope 文生图 API 文档:
 *  POST https://dashscope.aliyuncs.com/api/v1/services/aigc/text2image/image-synthesis
 *  GET  https://dashscope.aliyuncs.com/api/v1/tasks/{task_id}
 */
@Service
public class ImageGenService {

    private static final Logger log = LoggerFactory.getLogger(ImageGenService.class);

    private static final String SUBMIT_URL =
            "https://dashscope.aliyuncs.com/api/v1/services/aigc/text2image/image-synthesis";
    private static final String TASK_URL =
            "https://dashscope.aliyuncs.com/api/v1/tasks/";
    private static final String CACHE_PREFIX = "image:gen:";
    private static final long   CACHE_TTL_DAYS = 7;

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    private final StringRedisTemplate redis;
    private final ObjectMapper         objectMapper;
    private final RestTemplate         rest;

    public ImageGenService(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis        = redis;
        this.objectMapper = objectMapper;
        this.rest         = new RestTemplate();
    }

    // ── 提交任务 ─────────────────────────────────────────────────────────────

    public ImageGenResponse submitTask(String prompt, String type) {
        // 先查缓存
        String cacheKey = CACHE_PREFIX + sha256(prompt);
        String cached   = redis.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("[ImageGen] cache hit for prompt hash");
            return ImageGenResponse.succeeded("cached", cached);
        }

        // 构建增强 prompt（根据 type 自动注入末日风格引导词）
        String enhancedPrompt = buildPrompt(prompt, type);

        try {
            Map<String, Object> input = Map.of(
                    "prompt", enhancedPrompt
            );
            Map<String, Object> params = Map.of(
                    "style", "<anime>",
                    "size",  "1024*768",
                    "n",     1
            );
            Map<String, Object> body = Map.of(
                    "model",      "wanx-v1",
                    "input",      input,
                    "parameters", params
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization",    "Bearer " + apiKey);
            headers.set("X-DashScope-Async", "enable");   // 异步模式
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<String> resp = rest.exchange(
                    SUBMIT_URL, HttpMethod.POST,
                    new HttpEntity<>(objectMapper.writeValueAsString(body), headers),
                    String.class);

            JsonNode root = objectMapper.readTree(resp.getBody());
            String taskId = root.path("output").path("task_id").asText();
            log.info("[ImageGen] submitted task_id={}", taskId);
            return ImageGenResponse.pending(taskId);

        } catch (Exception e) {
            log.error("[ImageGen] submit failed: {}", e.getMessage());
            return ImageGenResponse.failed("error", "提交任务失败：" + e.getMessage());
        }
    }

    // ── 查询任务 ─────────────────────────────────────────────────────────────

    public ImageGenResponse queryTask(String taskId) {
        if ("cached".equals(taskId)) {
            return ImageGenResponse.failed(taskId, "请重新提交任务");
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey);

            ResponseEntity<String> resp = rest.exchange(
                    TASK_URL + taskId, HttpMethod.GET,
                    new HttpEntity<>(headers), String.class);

            JsonNode root   = objectMapper.readTree(resp.getBody());
            String status   = root.path("output").path("task_status").asText();

            return switch (status) {
                case "SUCCEEDED" -> {
                    String url = root.path("output")
                            .path("results").get(0)
                            .path("url").asText();
                    // 写入缓存（此处无 prompt，跳过 prompt-based cache；taskId 结果缓存）
                    redis.opsForValue().set(
                            "image:task:" + taskId, url,
                            CACHE_TTL_DAYS, TimeUnit.DAYS);
                    log.info("[ImageGen] task_id={} SUCCEEDED url={}", taskId, url);
                    yield ImageGenResponse.succeeded(taskId, url);
                }
                case "FAILED" -> {
                    String msg = root.path("output").path("message").asText("unknown");
                    log.warn("[ImageGen] task_id={} FAILED msg={}", taskId, msg);
                    yield ImageGenResponse.failed(taskId, msg);
                }
                case "RUNNING" -> ImageGenResponse.running(taskId);
                default        -> ImageGenResponse.pending(taskId);
            };

        } catch (Exception e) {
            log.error("[ImageGen] query failed taskId={}: {}", taskId, e.getMessage());
            return ImageGenResponse.failed(taskId, "查询任务失败：" + e.getMessage());
        }
    }

    // ── 私有工具 ─────────────────────────────────────────────────────────────

    /**
     * 根据 type 自动注入末日废土风格引导词，让 wanx-v1 生成贴合游戏氛围的图片
     */
    private String buildPrompt(String userPrompt, String type) {
        String base = switch (type) {
            case "cabin_bg" ->
                "post-apocalyptic cozy survival shelter interior, " +
                "warm lantern light, worn books on shelf, hand-drawn map pinned to wall, " +
                "vintage radio, canned food, sleeping bag on bed, tools scattered, " +
                "train window showing foggy ruins outside, lofi illustration style, " +
                "warm amber tones, detailed and atmospheric, top-down perspective";
            case "item" ->
                "single item icon, post-apocalyptic survival gear, " +
                "flat top-down view, transparent background, " +
                "pixel art style, warm muted colors, clean silhouette";
            default -> "post-apocalyptic atmosphere, lofi illustration";
        };
        // 用户自定义词追加在后面，优先级低于风格引导
        return base + (userPrompt.isBlank() ? "" : ", " + userPrompt);
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(input.getBytes()));
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }
}
