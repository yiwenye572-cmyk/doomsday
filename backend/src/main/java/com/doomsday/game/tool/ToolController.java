package com.doomsday.game.tool;

import com.doomsday.game.common.ApiResponse;
import com.doomsday.game.common.TraceIdSupport;
import com.doomsday.game.tool.dto.ToolCallRequest;
import com.doomsday.game.tool.dto.ToolCallResult;
import com.doomsday.game.tool.runtime.ToolAuditService;
import com.doomsday.game.tool.runtime.ToolAuditService.ToolSummary;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ToolController {

    private final ToolRegistry toolRegistry;
    private final ToolExecutor toolExecutor;
    private final ToolAuditService toolAuditService;

    public ToolController(ToolRegistry toolRegistry,
                          ToolExecutor toolExecutor,
                          ToolAuditService toolAuditService) {
        this.toolRegistry = toolRegistry;
        this.toolExecutor = toolExecutor;
        this.toolAuditService = toolAuditService;
    }

    @PostMapping("/api/v1/internal/tool/call")
    public ApiResponse<ToolCallResult> callTool(@Valid @RequestBody ToolCallRequest request) {
        ToolContext context = new ToolContext(
                null,
                request.traceId() == null || request.traceId().isBlank() ? TraceIdSupport.currentTraceId() : request.traceId(),
                request.callerAgent(),
                null,
                null
        );
        ToolCallResult result = toolExecutor.execute(context, request);
        return ApiResponse.ok(result, TraceIdSupport.currentTraceId());
    }

    @GetMapping("/api/v1/admin/tools")
    public ApiResponse<List<Map<String, Object>>> listTools() {
        List<Map<String, Object>> data = toolRegistry.all().stream()
                .map(tool -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("toolName", tool.name());
                    row.put("description", tool.description());
                    row.put("sideEffect", tool.sideEffect());
                    row.put("requiredFields", tool.requiredPayloadFields());
                    return row;
                })
                .toList();
        return ApiResponse.ok(data, TraceIdSupport.currentTraceId());
    }

    @GetMapping("/api/v1/admin/tools/summary")
    public ApiResponse<List<ToolSummary>> summary() {
        return ApiResponse.ok(toolAuditService.summarize(), TraceIdSupport.currentTraceId());
    }

    @GetMapping("/api/v1/admin/tools/audits")
    public ApiResponse<List<Map<String, Object>>> recentAudits(@RequestParam(defaultValue = "20") int limit) {
        int safeLimit = Math.max(1, Math.min(100, limit));
        List<Map<String, Object>> data = toolAuditService.recent().stream()
                .limit(safeLimit)
                .map(audit -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("traceId", audit.getTraceId());
                    row.put("sessionId", audit.getSessionId() == null ? "" : audit.getSessionId());
                    row.put("callerAgent", audit.getCallerAgent() == null ? "" : audit.getCallerAgent());
                    row.put("toolName", audit.getToolName());
                    row.put("status", audit.getStatus());
                    row.put("retryCount", audit.getRetryCount());
                    row.put("latencyMs", audit.getLatencyMs());
                    row.put("errorCode", audit.getErrorCode() == null ? "" : audit.getErrorCode());
                    row.put("compensated", audit.isCompensated());
                    row.put("createdAt", audit.getCreatedAt() == null ? "" : audit.getCreatedAt().toString());
                    return row;
                })
                .toList();
        return ApiResponse.ok(data, TraceIdSupport.currentTraceId());
    }
}
