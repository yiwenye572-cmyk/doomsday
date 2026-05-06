package com.doomsday.game.arbitration.dto;

public record ArbitrationResult(
        boolean pass,
        String finalAction,
        String reason,
        double riskScore,
        String ruleValidation,
        String semanticAlignment,
        String agentVoting
) {}
