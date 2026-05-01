package com.doomsday.game.api;

public record DifficultyDeltaPayload(
        double threat,
        double loot,
        double eventRate,
        double bossProbability
) {}
