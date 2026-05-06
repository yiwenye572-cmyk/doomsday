package com.doomsday.game.tool.runtime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ToolRetryPolicy {

    private final int maxRetries;

    public ToolRetryPolicy(@Value("${game.tool.max-retries:1}") int maxRetries) {
        this.maxRetries = Math.max(0, maxRetries);
    }

    public int maxRetries() {
        return maxRetries;
    }

    public boolean shouldRetry(Exception ex, int attempt) {
        if (attempt > maxRetries) {
            return false;
        }
        return ex instanceof RuntimeException;
    }
}
