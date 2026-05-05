package com.doomsday.game.media;

import jakarta.validation.constraints.NotBlank;

public record GenerateImageRequest(
        String sessionId,
        String traceId,
        @NotBlank(message = "prompt is required") String prompt,
        String style,
        Integer timeoutMs
) {}
