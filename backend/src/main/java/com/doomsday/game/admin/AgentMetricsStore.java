package com.doomsday.game.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis 指标存储：
 *  - game:metrics:agent:{agentName}    Hash  {
 *      totalCalls, successCalls, failCalls, totalMs,
 *      totalQueueWaitMs, totalModelMs, totalPostProcessMs,
 *      totalPromptTokens, totalCompletionTokens, totalTokens
 *    }
 *  - game:metrics:trace:{traceId}      String JSON  TraceDetail  TTL=2h
 *  - game:metrics:traces:index         List  traceId 列表，最近 200 条
 */
@Component
public class AgentMetricsStore {

    private static final Logger log = LoggerFactory.getLogger(AgentMetricsStore.class);
    private static final String AGENT_METRICS_PREFIX = "game:metrics:agent:";
    private static final String TRACE_PREFIX = "game:metrics:trace:";
    private static final String TRACE_INDEX_KEY = "game:metrics:traces:index";
    private static final int TRACE_INDEX_MAX = 200;
    private static final Duration TRACE_TTL = Duration.ofHours(2);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public AgentMetricsStore(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    /** 记录单个 Agent 调用结果（包含阶段耗时与 token）。 */
    public void recordAgentCall(String agentName,
                                long elapsedMs,
                                long queueWaitMs,
                                long modelMs,
                                long postProcessMs,
                                int promptTokens,
                                int completionTokens,
                                int totalTokens,
                                boolean success) {
        String key = AGENT_METRICS_PREFIX + agentName;
        try {
            redis.opsForHash().increment(key, "totalCalls", 1);
            redis.opsForHash().increment(key, "totalMs", elapsedMs);
            redis.opsForHash().increment(key, "totalQueueWaitMs", Math.max(0, queueWaitMs));
            redis.opsForHash().increment(key, "totalModelMs", Math.max(0, modelMs));
            redis.opsForHash().increment(key, "totalPostProcessMs", Math.max(0, postProcessMs));
            redis.opsForHash().increment(key, "totalPromptTokens", Math.max(0, promptTokens));
            redis.opsForHash().increment(key, "totalCompletionTokens", Math.max(0, completionTokens));
            redis.opsForHash().increment(key, "totalTokens", Math.max(0, totalTokens));
            if (success) {
                redis.opsForHash().increment(key, "successCalls", 1);
            } else {
                redis.opsForHash().increment(key, "failCalls", 1);
            }
        } catch (Exception e) {
            log.warn("[AgentMetricsStore] recordAgentCall failed for {}: {}", agentName, e.getMessage());
        }
    }

    /** 保存全链路 Trace 明细 */
    public void saveTrace(TraceDetail trace) {
        try {
            String key = TRACE_PREFIX + trace.traceId();
            redis.opsForValue().set(key, objectMapper.writeValueAsString(trace), TRACE_TTL);
            redis.opsForList().leftPush(TRACE_INDEX_KEY, trace.traceId());
            redis.opsForList().trim(TRACE_INDEX_KEY, 0, TRACE_INDEX_MAX - 1);
        } catch (Exception e) {
            log.warn("[AgentMetricsStore] saveTrace failed: {}", e.getMessage());
        }
    }

    /** 查询所有 Agent 聚合指标 */
    public List<AgentMetricsSummary> getAllAgentMetrics() {
        List<AgentMetricsSummary> result = new ArrayList<>();
        try {
            var keys = redis.keys(AGENT_METRICS_PREFIX + "*");
            if (keys == null) return result;
            for (String key : keys) {
                String agentName = key.substring(AGENT_METRICS_PREFIX.length());
                Map<Object, Object> raw = redis.opsForHash().entries(key);
                long total = parseLong(raw, "totalCalls");
                long success = parseLong(raw, "successCalls");
                long fail = parseLong(raw, "failCalls");
                long totalMs = parseLong(raw, "totalMs");
                long totalQueueWaitMs = parseLong(raw, "totalQueueWaitMs");
                long totalModelMs = parseLong(raw, "totalModelMs");
                long totalPostProcessMs = parseLong(raw, "totalPostProcessMs");
                long totalPromptTokens = parseLong(raw, "totalPromptTokens");
                long totalCompletionTokens = parseLong(raw, "totalCompletionTokens");
                long totalTokens = parseLong(raw, "totalTokens");
                double avgMs = total > 0 ? (double) totalMs / total : 0;
                double avgQueueWaitMs = total > 0 ? (double) totalQueueWaitMs / total : 0;
                double avgModelMs = total > 0 ? (double) totalModelMs / total : 0;
                double avgPostProcessMs = total > 0 ? (double) totalPostProcessMs / total : 0;
                double avgPromptTokens = total > 0 ? (double) totalPromptTokens / total : 0;
                double avgCompletionTokens = total > 0 ? (double) totalCompletionTokens / total : 0;
                double avgTokens = total > 0 ? (double) totalTokens / total : 0;
                double successRate = total > 0 ? (double) success / total : 0;
                result.add(new AgentMetricsSummary(
                        agentName,
                        total,
                        success,
                        fail,
                        avgMs,
                        avgQueueWaitMs,
                        avgModelMs,
                        avgPostProcessMs,
                        avgPromptTokens,
                        avgCompletionTokens,
                        avgTokens,
                        successRate
                ));
            }
        } catch (Exception e) {
            log.warn("[AgentMetricsStore] getAllAgentMetrics failed: {}", e.getMessage());
        }
        return result;
    }

    /** 查询最近 N 条 Trace */
    public List<TraceDetail> getRecentTraces(int limit) {
        List<TraceDetail> result = new ArrayList<>();
        try {
            List<String> traceIds = redis.opsForList().range(TRACE_INDEX_KEY, 0, limit - 1);
            if (traceIds == null) return result;
            for (String traceId : traceIds) {
                try {
                    String json = redis.opsForValue().get(TRACE_PREFIX + traceId);
                    if (json != null) {
                        result.add(objectMapper.readValue(json, TraceDetail.class));
                    }
                } catch (Exception decodeError) {
                    log.warn("[AgentMetricsStore] skip malformed trace {}: {}", traceId, decodeError.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("[AgentMetricsStore] getRecentTraces failed: {}", e.getMessage());
        }
        return result;
    }

    /** 按 traceId 查单条 */
    public TraceDetail getTrace(String traceId) {
        try {
            String json = redis.opsForValue().get(TRACE_PREFIX + traceId);
            if (json == null) return null;
            return objectMapper.readValue(json, TraceDetail.class);
        } catch (Exception e) {
            log.warn("[AgentMetricsStore] getTrace failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 最近窗口与前序窗口对比：用于展示优化前后效果。
     */
    public TraceMetricsComparison getTraceMetricsComparison(int windowSize) {
        int safeWindow = Math.max(5, Math.min(windowSize, 100));
        List<TraceDetail> traces = getRecentTraces(safeWindow * 2);
        List<TraceDetail> sorted = traces.stream()
                .sorted(Comparator.comparingLong(TraceDetail::startedAt).reversed())
                .toList();

        List<TraceDetail> currentWindow = sorted.stream().limit(safeWindow).toList();
        List<TraceDetail> previousWindow = sorted.stream().skip(safeWindow).limit(safeWindow).toList();

        TraceMetricsSnapshot current = buildSnapshot(currentWindow);
        TraceMetricsSnapshot previous = buildSnapshot(previousWindow);
        return new TraceMetricsComparison(current, previous);
    }

    private TraceMetricsSnapshot buildSnapshot(List<TraceDetail> traces) {
        if (traces == null || traces.isEmpty()) {
            return new TraceMetricsSnapshot(0, 0, 0, 0, 0, 0, 0, 0);
        }

        int total = traces.size();
        long okCount = traces.stream()
                .filter(t -> t.finalStatus() != null && "OK".equalsIgnoreCase(t.finalStatus()))
                .count();
        List<Long> elapsedList = traces.stream()
                .map(TraceDetail::elapsedMs)
                .sorted()
                .toList();
        long p95 = elapsedList.get(Math.min(elapsedList.size() - 1, (int) Math.floor(elapsedList.size() * 0.95)));
        double avgElapsed = traces.stream().mapToLong(TraceDetail::elapsedMs).average().orElse(0);
        long conflictCount = traces.stream().filter(t -> Boolean.TRUE.equals(t.conflictDetected())).count();
        long eventHitCount = traces.stream().filter(t -> Boolean.TRUE.equals(t.eventHit())).count();
        double avgEventHit = traces.stream()
                .map(TraceDetail::eventHitCount)
                .filter(v -> v != null)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0);
        double avgEventCandidates = traces.stream()
                .map(TraceDetail::eventCandidateCount)
                .filter(v -> v != null)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0);

        return new TraceMetricsSnapshot(
                total,
                okCount,
                total > 0 ? (double) okCount / total : 0,
                avgElapsed,
                p95,
                total > 0 ? (double) conflictCount / total : 0,
                total > 0 ? (double) eventHitCount / total : 0,
                avgEventCandidates > 0 ? Math.min(1.0, avgEventHit / avgEventCandidates) : 0
        );
    }

    private long parseLong(Map<Object, Object> map, String field) {
        Object v = map.get(field);
        if (v == null) return 0;
        try { return Long.parseLong(v.toString()); } catch (NumberFormatException e) { return 0; }
    }

    // ===== 值对象 =====

    public record AgentMetricsSummary(
            String agentName,
            long totalCalls,
            long successCalls,
            long failCalls,
            double avgMs,
            double avgQueueWaitMs,
            double avgModelMs,
            double avgPostProcessMs,
            double avgPromptTokens,
            double avgCompletionTokens,
            double avgTokens,
            double successRate
    ) {}

    public record TraceDetail(
            String traceId,
            String sessionId,
            int turn,
            long startedAt,
            long elapsedMs,
            String finalStatus,
            List<AgentSpan> spans,
            Boolean conflictDetected,
            Boolean eventHit,
            Integer eventHitCount,
            Integer eventCandidateCount
    ) {}

    public record TraceMetricsSnapshot(
            int sampleSize,
            long successCount,
            double successRate,
            double avgElapsedMs,
            long p95ElapsedMs,
            double conflictRate,
            double eventHitRate,
            double eventPrecision
    ) {}

    public record TraceMetricsComparison(
            TraceMetricsSnapshot current,
            TraceMetricsSnapshot previous
    ) {}

    public record AgentSpan(
            String agentName,
            long elapsedMs,
            String status,
            String errorMessage,
            Long queueWaitMs,
            Long modelMs,
            Long postProcessMs,
            Integer promptTokens,
            Integer completionTokens,
            Integer totalTokens,
            Double tokensPerSecond,
            String modelName
    ) {}
}
