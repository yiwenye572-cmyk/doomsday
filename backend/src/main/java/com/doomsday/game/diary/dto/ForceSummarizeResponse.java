package com.doomsday.game.diary.dto;

public record ForceSummarizeResponse(
        String sessionId,
        boolean created,
        int fromTurn,
        int toTurn,
        String level,
        String summary
) {
}
