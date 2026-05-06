package com.doomsday.game.arbitration;

import com.doomsday.game.agent.TurnContext;
import com.doomsday.game.arbitration.dto.ArbitrationRequest;
import com.doomsday.game.arbitration.dto.ArbitrationResult;
import org.springframework.stereotype.Component;

@Component
public class ConflictArbitrator {

    private final RuleValidationLayer ruleValidationLayer;
    private final SemanticAlignmentLayer semanticAlignmentLayer;
    private final AgentVotingLayer agentVotingLayer;

    public ConflictArbitrator(RuleValidationLayer ruleValidationLayer,
                              SemanticAlignmentLayer semanticAlignmentLayer,
                              AgentVotingLayer agentVotingLayer) {
        this.ruleValidationLayer = ruleValidationLayer;
        this.semanticAlignmentLayer = semanticAlignmentLayer;
        this.agentVotingLayer = agentVotingLayer;
    }

    public ArbitrationResult evaluate(TurnContext ctx) {
        double riskScore = resolveRiskScore(ctx);

        LayerOutcome rule = ruleValidationLayer.evaluate(ctx);
        if (!rule.pass()) {
            return new ArbitrationResult(
                    false,
                    "SWITCH_TO_SAFE_OPTIONS",
                    rule.reason(),
                    riskScore,
                    rule.code(),
                    "SKIPPED",
                    "SKIPPED"
            );
        }

        LayerOutcome semantic = semanticAlignmentLayer.evaluate(ctx);
        if (!semantic.pass()) {
            return new ArbitrationResult(
                    false,
                    "SWITCH_TO_SAFE_OPTIONS",
                    semantic.reason(),
                    riskScore,
                    rule.code(),
                    semantic.code(),
                    "SKIPPED"
            );
        }

        LayerOutcome voting = agentVotingLayer.evaluate(ctx, riskScore, true);
        if (!voting.pass()) {
            return new ArbitrationResult(
                    false,
                    "ABORT",
                    voting.reason(),
                    riskScore,
                    rule.code(),
                    semantic.code(),
                    voting.code()
            );
        }

        return new ArbitrationResult(
                true,
                "PASS",
                "ok",
                riskScore,
                rule.code(),
                semantic.code(),
                voting.code()
        );
    }

    public ArbitrationResult evaluate(ArbitrationRequest request) {
        LayerOutcome rule = ruleValidationLayer.evaluate(request);
        if (!rule.pass()) {
            return new ArbitrationResult(false, "SWITCH_EVENT", rule.reason(), request.riskScore(), rule.code(), "SKIPPED", "SKIPPED");
        }

        LayerOutcome semantic = semanticAlignmentLayer.evaluate(request);
        if (!semantic.pass()) {
            return new ArbitrationResult(false, "SWITCH_EVENT", semantic.reason(), request.riskScore(), rule.code(), semantic.code(), "SKIPPED");
        }

        LayerOutcome voting = agentVotingLayer.evaluate(request, semantic.pass());
        if (!voting.pass()) {
            return new ArbitrationResult(false, "REJECT", voting.reason(), request.riskScore(), rule.code(), semantic.code(), voting.code());
        }

        return new ArbitrationResult(true, "PASS", "ok", request.riskScore(), rule.code(), semantic.code(), voting.code());
    }

    private double resolveRiskScore(TurnContext ctx) {
        if (ctx.options == null || ctx.options.isEmpty()) {
            return 0.4;
        }
        long highRisk = ctx.options.stream()
                .map(option -> option.riskLevel() == null ? "" : option.riskLevel().toUpperCase())
                .filter(level -> level.contains("HIGH"))
                .count();
        return Math.min(1.0, 0.35 + highRisk * 0.2);
    }
}
