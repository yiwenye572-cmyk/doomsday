package com.doomsday.game.agent.impl;

import com.doomsday.game.agent.AgentChain;
import com.doomsday.game.agent.AgentHandler;
import com.doomsday.game.agent.TurnContext;
import com.doomsday.game.api.StateDeltaPayload;
import com.doomsday.game.domain.GameSession;
import com.doomsday.game.domain.SessionRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * State Commit Agent：将仲裁后的状态变更写入 Redis，做版本递增与 diff 记录。
 */
@Component
public class StateCommitAgent implements AgentHandler {

    private static final Logger log = LoggerFactory.getLogger(StateCommitAgent.class);

    private final SessionRepository sessionRepo;

    public StateCommitAgent(SessionRepository sessionRepo) {
        this.sessionRepo = sessionRepo;
    }

    @Override
    public String name() {
        return "StateCommitAgent";
    }

    @Override
    public void handle(TurnContext ctx, AgentChain next) {
        GameSession s = ctx.session;

        // 应用本回合消耗（体力 -6，来自 P0 设计，后续从 difficultyDelta 动态调整）
        int staminaCost = resolveStaminaCost(ctx);
        s.setStamina(Math.max(0, s.getStamina() - staminaCost));
        s.setTurn(s.getTurn() + 1);
        s.setCurrentTurn(s.getTurn());
        s.setVersion(s.getVersion() + 1);
        s.setCurrentOptions(ctx.options);
        s.setChallengeIndex(computeChallengeIndex(s));

        // 状态 diff 摘要
        ctx.stateDelta = new StateDeltaPayload(-staminaCost, 12, List.of("found_medical_trace"));

        // 持久化
        sessionRepo.save(s);
        log.debug("[{}] traceId={} session={} turn={} v={}",
                name(), ctx.traceId, s.getSessionId(), s.getTurn(), s.getVersion());

        next.handle(ctx);
    }

    private int resolveStaminaCost(TurnContext ctx) {
        // 根据难度 delta 决定体力消耗（threat 越高消耗越多）
        if (ctx.difficultyDelta != null && ctx.difficultyDelta.threat() > 0.05) {
            return 8;
        }
        return 6;
    }

    private double computeChallengeIndex(GameSession s) {
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
