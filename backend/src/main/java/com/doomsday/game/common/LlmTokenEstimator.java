package com.doomsday.game.common;

/**
 * 轻量 token 估算器。
 * 在供应商未返回 usage 时，先用字符长度估算，保证可观测性链路完整。
 */
public final class LlmTokenEstimator {

    private LlmTokenEstimator() {}

    public static int estimatePromptTokens(String prompt) {
        return estimateByChars(prompt);
    }

    public static int estimateCompletionTokens(String text) {
        return estimateByChars(text);
    }

    public static int estimateTotalTokens(String prompt, String text) {
        return estimatePromptTokens(prompt) + estimateCompletionTokens(text);
    }

    private static int estimateByChars(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        // 对中英文混合文本取保守估算：约每 2 个字符≈1 token。
        return Math.max(1, (text.length() + 1) / 2);
    }
}
