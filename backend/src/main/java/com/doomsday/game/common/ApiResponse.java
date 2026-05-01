package com.doomsday.game.common;

public record ApiResponse<T>(
        String code,
        String message,
        T data,
        String traceId,
        long timestamp
) {
    public static <T> ApiResponse<T> ok(T data, String traceId) {
        return new ApiResponse<>("OK", "success", data, traceId, System.currentTimeMillis());
    }

    public static <T> ApiResponse<T> fail(String code, String message, T data, String traceId) {
        return new ApiResponse<>(code, message, data, traceId, System.currentTimeMillis());
    }
}
