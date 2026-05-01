package com.doomsday.game.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record ComebackCardRequest(
        @PositiveOrZero long expectedVersion,
        @NotBlank String reason
) {}
