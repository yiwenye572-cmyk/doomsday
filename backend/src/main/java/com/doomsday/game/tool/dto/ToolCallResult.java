package com.doomsday.game.tool.dto;

import java.util.Map;

public record ToolCallResult(
        String toolName,
        String status,
        int retryCount,
        long latencyMs,
        Map<String, Object> result,
        String errorCode,
        String errorMessage,
        boolean compensated
) {
    public boolean success() {
        return "SUCCESS".equals(status);
    }
}
