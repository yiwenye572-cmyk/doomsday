package com.doomsday.game.admin;

import com.doomsday.game.common.ApiResponse;
import com.doomsday.game.common.TraceIdSupport;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端可观测性接口：
 *   GET /api/v1/admin/metrics/agents          — 所有 Agent 聚合指标
 *   GET /api/v1/admin/metrics/traces          — 最近 N 条 Trace 明细
 *   GET /api/v1/admin/metrics/traces/{traceId} — 单条 Trace 详情
 */
@RestController
@RequestMapping("/api/v1/admin/metrics")
public class AdminMetricsController {

    private final AgentMetricsStore metricsStore;

    public AdminMetricsController(AgentMetricsStore metricsStore) {
        this.metricsStore = metricsStore;
    }

    @GetMapping("/agents")
    public ApiResponse<List<AgentMetricsStore.AgentMetricsSummary>> getAgentMetrics() {
        return ApiResponse.ok(metricsStore.getAllAgentMetrics(), TraceIdSupport.currentTraceId());
    }

    @GetMapping("/traces")
    public ApiResponse<List<AgentMetricsStore.TraceDetail>> getRecentTraces(
            @RequestParam(defaultValue = "20") int limit) {
        int safeLimit = Math.min(limit, 100);
        return ApiResponse.ok(metricsStore.getRecentTraces(safeLimit), TraceIdSupport.currentTraceId());
    }

    @GetMapping("/traces/{traceId}")
    public ApiResponse<AgentMetricsStore.TraceDetail> getTrace(@PathVariable String traceId) {
        AgentMetricsStore.TraceDetail detail = metricsStore.getTrace(traceId);
        if (detail == null) {
            return ApiResponse.fail("NOT_FOUND", "trace not found", null, TraceIdSupport.currentTraceId());
        }
        return ApiResponse.ok(detail, TraceIdSupport.currentTraceId());
    }
}
