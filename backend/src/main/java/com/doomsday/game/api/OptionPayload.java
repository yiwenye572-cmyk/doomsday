package com.doomsday.game.api;

public record OptionPayload(
        String id,
        String text,
        String riskLevel,
        String expectedEffect
) {}
