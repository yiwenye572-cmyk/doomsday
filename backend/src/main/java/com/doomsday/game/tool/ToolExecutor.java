package com.doomsday.game.tool;

import com.doomsday.game.common.ApiException;
import com.doomsday.game.tool.dto.ToolCallRequest;
import com.doomsday.game.tool.dto.ToolCallResult;
import com.doomsday.game.tool.runtime.ToolAuditService;
import com.doomsday.game.tool.runtime.ToolCompensationHandler;
import com.doomsday.game.tool.runtime.ToolRetryPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Component;

@Component
public class ToolExecutor {

    private final ToolRegistry registry;
    private final ToolRetryPolicy retryPolicy;
    private final ToolCompensationHandler compensationHandler;
    private final ToolAuditService auditService;

    public ToolExecutor(ToolRegistry registry,
                        ToolRetryPolicy retryPolicy,
                        ToolCompensationHandler compensationHandler,
                        ToolAuditService auditService) {
        this.registry = registry;
        this.retryPolicy = retryPolicy;
        this.compensationHandler = compensationHandler;
        this.auditService = auditService;
    }

    /**
     * 批量执行工具：
     * - 有副作用工具严格串行，保持行为稳定。
     * - 只读工具可并发执行，降低总体等待时间。
     */
    public List<ToolCallResult> executeBatch(ToolContext context,
                                             List<ToolCallRequest> requests,
                                             boolean allowParallelReadOnly) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        if (!allowParallelReadOnly) {
            List<ToolCallResult> seq = new ArrayList<>(requests.size());
            for (ToolCallRequest request : requests) {
                seq.add(execute(withCaller(context, request), request));
            }
            return seq;
        }

        List<ToolCallResult> ordered = new ArrayList<>(requests.size());
        for (int i = 0; i < requests.size(); i++) {
            ordered.add(null);
        }

        Map<Integer, CompletableFuture<ToolCallResult>> futures = new HashMap<>();
        for (int i = 0; i < requests.size(); i++) {
            ToolCallRequest request = requests.get(i);
            if (isReadOnlyTool(request.toolName())) {
                int index = i;
                futures.put(index, CompletableFuture.supplyAsync(() -> execute(withCaller(context, request), request)));
            }
        }

        for (int i = 0; i < requests.size(); i++) {
            ToolCallRequest request = requests.get(i);
            if (!isReadOnlyTool(request.toolName())) {
                ordered.set(i, execute(withCaller(context, request), request));
            }
        }

        for (Map.Entry<Integer, CompletableFuture<ToolCallResult>> entry : futures.entrySet()) {
            ordered.set(entry.getKey(), entry.getValue().join());
        }
        return ordered;
    }

    public ToolCallResult execute(ToolContext context, ToolCallRequest request) {
        long start = System.currentTimeMillis();
        ToolCallResult result;

        ToolDefinition tool = registry.find(request.toolName())
                .orElseThrow(() -> new ApiException("BAD_REQUEST", "tool not found: " + request.toolName()));

        List<String> validationErrors = tool.validate(request.payload());
        if (!validationErrors.isEmpty()) {
            result = new ToolCallResult(
                    request.toolName(),
                    "FAILED",
                    0,
                    System.currentTimeMillis() - start,
                    Map.of(),
                    "TOOL_SCHEMA_INVALID",
                    String.join("; ", validationErrors),
                    false
            );
            auditService.record(context, request, result);
            return result;
        }

        int attempt = 0;
        int retries = 0;
        Object snapshot = tool.sideEffect() ? compensationHandler.captureSnapshot(tool.name(), context) : null;
        Exception lastError = null;

        while (attempt <= retryPolicy.maxRetries()) {
            attempt++;
            try {
                Map<String, Object> output = tool.execute(context, safePayload(request.payload()));
                result = new ToolCallResult(
                        tool.name(),
                        "SUCCESS",
                        retries,
                        System.currentTimeMillis() - start,
                        output,
                        null,
                        null,
                        false
                );
                auditService.record(context, request, result);
                return result;
            } catch (Exception ex) {
                lastError = ex;
                if (retryPolicy.shouldRetry(ex, attempt)) {
                    retries++;
                    continue;
                }
                break;
            }
        }

        boolean compensated = false;
        if (tool.sideEffect()) {
            compensated = compensationHandler.compensate(tool.name(), context, snapshot);
        }

        result = new ToolCallResult(
                tool.name(),
                "FAILED",
                retries,
                System.currentTimeMillis() - start,
                Map.of(),
                "TOOL_EXEC_FAILED",
                lastError == null ? "unknown tool error" : lastError.getMessage(),
                compensated
        );
        auditService.record(context, request, result);
        return result;
    }

    private Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? Map.of() : payload;
    }

    private boolean isReadOnlyTool(String toolName) {
        return registry.find(toolName)
                .map(def -> !def.sideEffect())
                .orElse(false);
    }

    private ToolContext withCaller(ToolContext base, ToolCallRequest request) {
        return new ToolContext(
                base.sessionId(),
                base.traceId(),
                request.callerAgent(),
                base.session(),
                base.turnContext()
        );
    }
}
