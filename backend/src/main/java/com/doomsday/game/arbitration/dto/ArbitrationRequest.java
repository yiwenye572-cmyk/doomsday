package com.doomsday.game.arbitration.dto;

import java.util.Map;

public record ArbitrationRequest(
        String sessionId,
        String candidateEventId,
        double riskScore,
        Map<String, Object> stateSnapshot
) {}
