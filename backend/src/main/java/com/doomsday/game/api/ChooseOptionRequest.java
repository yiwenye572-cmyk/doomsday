package com.doomsday.game.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record ChooseOptionRequest(
        @PositiveOrZero long expectedVersion,
        @NotBlank String optionId
) {}
