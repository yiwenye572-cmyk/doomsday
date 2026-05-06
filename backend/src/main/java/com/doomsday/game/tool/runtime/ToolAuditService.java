package com.doomsday.game.tool.runtime;

import com.doomsday.game.tool.ToolContext;
import com.doomsday.game.tool.dto.ToolCallRequest;
import com.doomsday.game.tool.dto.ToolCallResult;
import com.doomsday.game.tool.model.ToolCallAudit;
import com.doomsday.game.tool.repo.ToolCallAuditRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ToolAuditService {

    private final ToolCallAuditRepository auditRepository;
    private final ObjectMapper objectMapper;

    public ToolAuditService(ToolCallAuditRepository auditRepository, ObjectMapper objectMapper) {
        this.auditRepository = auditRepository;
        this.objectMapper = objectMapper;
    }

    public void record(ToolContext context, ToolCallRequest request, ToolCallResult result) {
        ToolCallAudit audit = new ToolCallAudit();
        audit.setTraceId(context == null || context.traceId() == null ? "unknown" : context.traceId());
        audit.setSessionId(context == null ? null : context.sessionId());
        audit.setCallerAgent(request == null ? null : request.callerAgent());
        audit.setToolName(result == null ? (request == null ? "UNKNOWN" : request.toolName()) : result.toolName());
        audit.setStatus(result == null ? "FAILED" : result.status());
        audit.setRetryCount(result == null ? 0 : result.retryCount());
        audit.setLatencyMs(result == null ? 0 : result.latencyMs());
        audit.setErrorCode(result == null ? "TOOL_EXEC_FAILED" : result.errorCode());
        audit.setErrorMessage(result == null ? "unknown error" : result.errorMessage());
        audit.setRequestJson(toJson(request));
        audit.setResultJson(toJson(result == null ? null : result.result()));
        audit.setCompensated(result != null && result.compensated());
        auditRepository.save(audit);
    }

    public List<ToolSummary> summarize() {
        return auditRepository.aggregateSummary().stream()
                .map(row -> new ToolSummary(
                        asString(row[0]),
                        asLong(row[1]),
                        asLong(row[2]),
                        asLong(row[3]),
                        asDouble(row[4]),
                        asDouble(row[5])
                ))
                .toList();
    }

    public List<ToolCallAudit> recent() {
        return auditRepository.findTop50ByOrderByCreatedAtDesc();
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String asString(Object value) {
        return value == null ? "UNKNOWN" : value.toString();
    }

    private long asLong(Object value) {
        if (value == null) {
            return 0;
        }
        return ((Number) value).longValue();
    }

    private double asDouble(Object value) {
        if (value == null) {
            return 0;
        }
        return ((Number) value).doubleValue();
    }

    public record ToolSummary(
            String toolName,
            long totalCalls,
            long successCalls,
            long failedCalls,
            double avgMs,
            double avgRetry
    ) {
    }
}
