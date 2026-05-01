package com.doomsday.game.api;

import com.doomsday.game.domain.Difficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSessionRequest(
        @NotBlank String playerId,
        @NotNull Difficulty difficulty,
        @NotBlank String worldVersion,
        @NotBlank String styleProfile
) {}
