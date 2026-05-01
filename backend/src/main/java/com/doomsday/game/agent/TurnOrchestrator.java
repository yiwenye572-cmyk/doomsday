package com.doomsday.game.agent;

import com.doomsday.game.agent.impl.DifficultyDirectorAgent;
import com.doomsday.game.agent.impl.NarrationAgent;
import com.doomsday.game.agent.impl.OptionGenerationAgent;
import com.doomsday.game.agent.impl.PlotGenerationAgent;
import com.doomsday.game.agent.impl.RetrievalAgent;
import com.doomsday.game.agent.impl.RouterAgent;
import com.doomsday.game.agent.impl.RuleGuardAgent;
import com.doomsday.game.agent.impl.StateCommitAgent;
import com.doomsday.game.common.ApiException;
import com.doomsday.game.domain.GameSession;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 回合编排器：组装责任链并驱动单回合生命周期。
 *
 * 链路顺序（对应计划书 §5.1.2）：
 * Router → Retrieval → DifficultyDirector → Plot → Option → RuleGuard → StateCommit → Narration
 */
@Component
public class TurnOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(TurnOrchestrator.class);

    private final List<AgentHandler> pipeline;

    public TurnOrchestrator(
            RouterAgent router,
            RetrievalAgent retrieval,
            DifficultyDirectorAgent difficultyDirector,
            PlotGenerationAgent plot,
            OptionGenerationAgent option,
            RuleGuardAgent ruleGuard,
            StateCommitAgent stateCommit,
            NarrationAgent narration) {
        // 顺序即链路，修改此列表即可调整节点顺序
        this.pipeline = List.of(
                router, retrieval, difficultyDirector,
                plot, option, ruleGuard, stateCommit, narration
        );
    }

    /**
     * 执行单回合完整链路。
     *
     * @return 填充完毕的 TurnContext
     * @throws ApiException 若链路被 abort
     */
    public TurnContext run(String sessionId, GameSession session,
                           String playerInput, String idempotencyKey) {

        String traceId = "trace_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        TurnContext ctx = new TurnContext(sessionId, session, playerInput, idempotencyKey, traceId);

        log.info("[Orchestrator] START sessionId={} turn={} traceId={}", sessionId, session.getTurn() + 1, traceId);
        long t0 = System.currentTimeMillis();

        new AgentChain(pipeline).handle(ctx);

        long elapsed = System.currentTimeMillis() - t0;
        if (ctx.aborted) {
            log.warn("[Orchestrator] ABORTED traceId={} reason={} elapsed={}ms",
                    traceId, ctx.abortReason, elapsed);
            throw new ApiException("AGENT_ABORT", ctx.abortReason);
        }

        log.info("[Orchestrator] DONE traceId={} turn={} v={} elapsed={}ms",
                traceId, session.getTurn(), session.getVersion(), elapsed);
        return ctx;
    }
}
