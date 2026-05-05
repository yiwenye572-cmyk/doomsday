package com.doomsday.game.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis 指标存储：
 *  - game:metrics:agent:{agentName}    Hash  { totalCalls, successCalls, failCalls, totalMs }
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

    /** 记录单个 Agent 调用结果 */
    public void recordAgentCall(String agentName, long elapsedMs, boolean success) {
        String key = AGENT_METRICS_PREFIX + agentName;
        try {
            redis.opsForHash().increment(key, "totalCalls", 1);
            redis.opsForHash().increment(key, "totalMs", elapsedMs);
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
                double avgMs = total > 0 ? (double) totalMs / total : 0;
                double successRate = total > 0 ? (double) success / total : 0;
                result.add(new AgentMetricsSummary(agentName, total, success, fail, avgMs, successRate));
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
                String json = redis.opsForValue().get(TRACE_PREFIX + traceId);
                if (json != null) {
                    result.add(objectMapper.readValue(json, TraceDetail.class));
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
            double successRate
    ) {}

    public record TraceDetail(
            String traceId,
            String sessionId,
            int turn,
            long startedAt,
            long elapsedMs,
            String finalStatus,
            List<AgentSpan> spans
    ) {}

    public record AgentSpan(
            String agentName,
            long elapsedMs,
            String status,
            String errorMessage
    ) {}
}
