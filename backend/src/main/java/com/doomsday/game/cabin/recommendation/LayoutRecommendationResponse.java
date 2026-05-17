package com.doomsday.game.cabin.recommendation;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * GET /cabin/recommendation 响应体
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LayoutRecommendationResponse(
        String recommendationId,   // Redis key 中的唯一 ID（UUID）
        String sessionId,
        List<RecommendedItem> items,
        String reason,
        double confidence,
        String status              // "READY" | "ACCEPTED" | "REJECTED"
) {}
