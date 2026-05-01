package com.doomsday.game.api;

public record CreateSessionResponse(
        String sessionId,
        SessionStateResponse initialState,
        double[] challengeBand
) {}
