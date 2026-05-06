package com.doomsday.game.arbitration;

import com.doomsday.game.agent.TurnContext;
import com.doomsday.game.arbitration.dto.ArbitrationRequest;
import com.doomsday.game.api.OptionPayload;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AgentVotingLayer {

    public LayerOutcome evaluate(TurnContext ctx, double riskScore, boolean semanticPass) {
        if (riskScore < 0.7) {
            return LayerOutcome.pass("VOTING_SKIPPED");
        }

        int yes = 0;
        int no = 0;

        // voter A: 结果可执行性（至少有 4 个候选动作）
        if (ctx.options != null && ctx.options.size() == 4) {
            yes++;
        } else {
            no++;
        }

        // voter B: 风险合理性（高风险选项不过多）
        long highRisk = (ctx.options == null ? List.<OptionPayload>of() : ctx.options).stream()
                .map(OptionPayload::riskLevel)
                .filter(level -> level != null && level.toUpperCase().contains("HIGH"))
                .count();
        if (highRisk <= 2) {
            yes++;
        } else {
            no++;
        }

        // voter C: 语义对齐
        if (semanticPass) {
            yes++;
        } else {
            no++;
        }

        return yes >= 2
                ? LayerOutcome.pass("VOTING_PASS")
                : LayerOutcome.fail("VOTING_REJECT", "high risk voting rejected");
    }

    public LayerOutcome evaluate(ArbitrationRequest request, boolean semanticPass) {
        if (request.riskScore() < 0.7) {
            return LayerOutcome.pass("VOTING_SKIPPED");
        }
        if (!semanticPass) {
            return LayerOutcome.fail("VOTING_REJECT", "semantic alignment rejected");
        }
        return LayerOutcome.pass("VOTING_PASS");
    }
}
