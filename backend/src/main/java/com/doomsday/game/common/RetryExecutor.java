package com.doomsday.game.common;

import java.util.function.Supplier;

public final class RetryExecutor {

    private RetryExecutor() {
    }

    public static <T> T run(int maxAttempts, Supplier<T> task) {
        int attempts = Math.max(1, maxAttempts);
        RuntimeException last = null;
        for (int i = 1; i <= attempts; i++) {
            try {
                return task.get();
            } catch (RuntimeException ex) {
                last = ex;
                if (i == attempts) {
                    throw ex;
                }
            }
        }
        throw last == null ? new IllegalStateException("retry execution failed") : last;
    }
}
