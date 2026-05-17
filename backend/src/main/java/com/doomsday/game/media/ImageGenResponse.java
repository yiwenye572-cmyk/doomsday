package com.doomsday.game.media;

/**
 * 图片生成任务响应
 */
public record ImageGenResponse(
        String taskId,
        String status,    // PENDING | RUNNING | SUCCEEDED | FAILED
        String imageUrl,  // 仅 SUCCEEDED 时非 null
        String message    // 错误信息（FAILED 时使用）
) {
    public static ImageGenResponse pending(String taskId) {
        return new ImageGenResponse(taskId, "PENDING", null, null);
    }

    public static ImageGenResponse succeeded(String taskId, String imageUrl) {
        return new ImageGenResponse(taskId, "SUCCEEDED", imageUrl, null);
    }

    public static ImageGenResponse failed(String taskId, String message) {
        return new ImageGenResponse(taskId, "FAILED", null, message);
    }

    public static ImageGenResponse running(String taskId) {
        return new ImageGenResponse(taskId, "RUNNING", null, null);
    }
}
