package com.doomsday.game.media;

public record GenerateImageResponse(
        String imageUrl,
        String source,
        boolean fallback,
        String fallbackReason,
        String provider,
        long latencyMs
) {}
