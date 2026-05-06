package com.doomsday.game.arbitration;

import com.doomsday.game.agent.TurnContext;
import com.doomsday.game.arbitration.dto.ArbitrationRequest;
import org.springframework.stereotype.Component;

@Component
public class RuleValidationLayer {

    public LayerOutcome evaluate(TurnContext ctx) {
        if (ctx.rulesPassed) {
            return LayerOutcome.pass("RULE_PASS");
        }
        String reason = ctx.violations == null || ctx.violations.isEmpty()
                ? "rule validation failed"
                : String.join(";", ctx.violations);
        return LayerOutcome.fail("RULE_FAIL", reason);
    }

    public LayerOutcome evaluate(ArbitrationRequest request) {
        if (request.stateSnapshot() == null) {
            return LayerOutcome.fail("RULE_FAIL", "stateSnapshot is required");
        }
        Object infection = request.stateSnapshot().get("infection");
        if (infection instanceof Number n && n.intValue() >= 80) {
            return LayerOutcome.fail("RULE_FAIL", "infection_too_high_for_event");
        }
        return LayerOutcome.pass("RULE_PASS");
    }
}
