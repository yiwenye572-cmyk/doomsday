package com.doomsday.game.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record SubmitTurnRequest(
        @PositiveOrZero long expectedVersion,
        @NotBlank String playerInput,
        @PositiveOrZero long clientTime
) {}
