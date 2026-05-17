package com.doomsday.game.cabin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * GET /items/{itemId}/story 响应体
 * - status=PENDING/RUNNING → story=null
 * - status=DONE            → story=叙事文本
 * - status=FAILED          → story=null, errorMessage 可见
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ItemStoryResponse(
        Long taskId,
        String sessionId,
        String itemId,
        String status,       // PENDING | RUNNING | DONE | FAILED
        String story,
        String errorMessage,
        Instant generatedAt  // 仅 DONE 时有值（取 updated_at）
) {}
