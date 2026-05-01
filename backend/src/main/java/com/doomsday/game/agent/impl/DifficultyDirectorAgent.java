package com.doomsday.game.agent.impl;

import com.doomsday.game.agent.AgentChain;
import com.doomsday.game.agent.AgentHandler;
import com.doomsday.game.agent.TurnContext;
import com.doomsday.game.api.DifficultyDeltaPayload;
import com.doomsday.game.domain.Difficulty;
import com.doomsday.game.domain.GameSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Difficulty Director Agent（动态老虎机）：
 * 根据玩家当前状态与历史表现，计算难度调节量 difficulty_delta，
 * 重排后续事件权重（P1 版：纯规则调度，无模型）。
 */
@Component
public class DifficultyDirectorAgent implements AgentHandler {

    private static final Logger log = LoggerFactory.getLogger(DifficultyDirectorAgent.class);

    @Override
    public String name() {
        return "DifficultyDirectorAgent";
    }

    @Override
    public void handle(TurnContext ctx, AgentChain next) {
        GameSession s = ctx.session;
        double[] band = s.getDifficulty().challengeBand();
        double idx = calculateChallengeIndex(s);

        DifficultyDeltaPayload delta;
        if (idx < band[0]) {
            // 挑战不足：加压
            delta = new DifficultyDeltaPayload(0.10, -0.04, 0.06, 0.00);
        } else if (idx > band[1]) {
            // 压力过高：减压 + 恢复窗口
            delta = new DifficultyDeltaPayload(-0.08, 0.08, -0.04, 0.00);
        } else {
            // 在区间内：轻微扰动保持张力
            delta = new DifficultyDeltaPayload(0.02, -0.01, 0.01, 0.00);
        }

        ctx.difficultyDelta = delta;
        log.debug("[{}] traceId={} challengeIdx={} band=[{},{}] threat={}",
                name(), ctx.traceId, String.format("%.3f", idx),
                band[0], band[1], delta.threat());

        next.handle(ctx);
    }

    private double calculateChallengeIndex(GameSession s) {
        double base = switch (s.getDifficulty()) {
            case SEEKER -> 0.45;
            case SURVIVOR -> 0.58;
            case HELL -> 0.72;
        };
        double staminaFactor = (100 - s.getStamina()) / 200.0;
        double infectionFactor = s.getInfection() / 200.0;
        return Math.max(0.1, Math.min(0.95, base + staminaFactor + infectionFactor));
    }
}
