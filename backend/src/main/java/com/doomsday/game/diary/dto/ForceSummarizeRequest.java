package com.doomsday.game.diary.dto;

import jakarta.validation.constraints.NotBlank;

public record ForceSummarizeRequest(
        @NotBlank String sessionId,
        Integer fromTurn,
        Integer toTurn
) {
}
