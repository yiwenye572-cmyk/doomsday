package com.doomsday.game.api;

import java.util.List;

public record SessionStateResponse(
        String sessionId,
        long version,
        int hp,
        int stamina,
        int infection,
        String location,
        List<String> inventory,
        double challengeIndex,
        double[] challengeBand,
        int turn
) {}
