package com.doomsday.game.api;

public record ChooseOptionResponse(
        int turn,
        String selected,
        boolean applied,
        long newVersion,
        StateDeltaPayload stateDelta
) {}
