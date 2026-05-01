package com.doomsday.game.api;

import java.util.Map;

public record ComebackCardResponse(
        boolean applied,
        long newVersion,
        Map<String, String> effect,
        int remainingCount
) {}
