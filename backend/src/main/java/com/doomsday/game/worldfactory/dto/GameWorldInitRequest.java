package com.doomsday.game.worldfactory.dto;

import jakarta.validation.constraints.NotBlank;

public record GameWorldInitRequest(
        @NotBlank String worldTheme,
        @NotBlank String eraStyle,
        @NotBlank String survivalTone,
        String keyFaction,
        String forbiddenRule
) {}
