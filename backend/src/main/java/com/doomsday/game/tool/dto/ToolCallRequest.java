package com.doomsday.game.tool.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record ToolCallRequest(
        @NotBlank String idempotencyKey,
        String traceId,
        @NotBlank String callerAgent,
        @NotBlank String toolName,
        Integer timeoutMs,
        Map<String, Object> payload
) {
}
