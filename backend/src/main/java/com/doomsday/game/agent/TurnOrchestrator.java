package com.doomsday.game.agent;

import com.doomsday.game.admin.AgentMetricsStore;
import com.doomsday.game.agent.impl.DifficultyDirectorAgent;
import com.doomsday.game.agent.impl.NarrationAgent;
import com.doomsday.game.agent.impl.OptionGenerationAgent;
import com.doomsday.game.agent.impl.PlotGenerationAgent;
import com.doomsday.game.agent.impl.RetrievalAgent;
import com.doomsday.game.agent.impl.RouterAgent;
import com.doomsday.game.agent.impl.RuleGuardAgent;
import com.doomsday.game.agent.impl.StateCommitAgent;
import com.doomsday.game.api.OptionPayload;
import com.doomsday.game.api.PlotPayload;
import com.doomsday.game.arbitration.ConflictArbitrator;
import com.doomsday.game.arbitration.dto.ArbitrationResult;
import com.doomsday.game.common.ApiException;
import com.doomsday.game.domain.GameSession;
import com.doomsday.game.domain.SessionRepository;
import com.doomsday.game.domain.TurnMemory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

    private final SessionRepository sessionRepo;
    private final List<AgentHandler> preCommitPipeline;
    private final List<AgentHandler> postCommitPipeline;
    private final AgentMetricsStore metricsStore;
    private final ConflictArbitrator conflictArbitrator;

    public TurnOrchestrator(
            RouterAgent router,
            RetrievalAgent retrieval,
            DifficultyDirectorAgent difficultyDirector,
            PlotGenerationAgent plot,
            OptionGenerationAgent option,
            RuleGuardAgent ruleGuard,
            StateCommitAgent stateCommit,
            NarrationAgent narration,
            SessionRepository sessionRepo,
            AgentMetricsStore metricsStore,
            ConflictArbitrator conflictArbitrator) {
        this.sessionRepo = sessionRepo;
        this.metricsStore = metricsStore;
        this.conflictArbitrator = conflictArbitrator;
        // 前半段：直到 RuleGuard 完成校验，由 Orchestrator 做仲裁。
        this.preCommitPipeline = List.of(
                router, retrieval, difficultyDirector,
                plot, option, ruleGuard
        );
        this.postCommitPipeline = List.of(stateCommit, narration);
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
        injectRollingMemory(ctx);
        injectEpisodicSummary(ctx);

        log.info("[Orchestrator] START sessionId={} turn={} traceId={}", sessionId, session.getTurn() + 1, traceId);
        long t0 = System.currentTimeMillis();

        new AgentChain(preCommitPipeline).handle(ctx);

        if (!ctx.rulesPassed) {
            if (canFallback(ctx.violations)) {
                applyFallback(ctx);
                log.warn("[Orchestrator] FALLBACK traceId={} violations={}", traceId, ctx.violations);
            } else {
                ctx.abort("RULE_GUARD_BLOCK: " + String.join(";", ctx.violations));
            }
        }

        if (!ctx.aborted) {
            ArbitrationResult arbitration = conflictArbitrator.evaluate(ctx);
            ctx.extras.put("arbitration", arbitration);
            if (!arbitration.pass()) {
                if ("SWITCH_TO_SAFE_OPTIONS".equals(arbitration.finalAction())) {
                    applyFallback(ctx);
                } else {
                    ctx.abort("ARBITRATION_BLOCK: " + arbitration.reason());
                }
            }
        }

        if (!ctx.aborted) {
            new AgentChain(postCommitPipeline).handle(ctx);
        }

        long elapsed = System.currentTimeMillis() - t0;
        if (ctx.aborted) {
            log.warn("[Orchestrator] ABORTED traceId={} reason={} elapsed={}ms",
                    traceId, ctx.abortReason, elapsed);
            saveTrace(ctx, t0, elapsed, "ABORTED");
            throw new ApiException("AGENT_ABORT", ctx.abortReason);
        }

        appendRollingMemory(ctx);

        log.info("[Orchestrator] DONE traceId={} turn={} v={} elapsed={}ms",
                traceId, session.getTurn(), session.getVersion(), elapsed);
        saveTrace(ctx, t0, elapsed, "OK");
        return ctx;
    }

    private void injectRollingMemory(TurnContext ctx) {
        List<TurnMemory> memories = sessionRepo.findRecentTurnMemories(ctx.sessionId, 4);
        if (memories.isEmpty()) {
            return;
        }

        List<TurnContext.L0MemorySummary> memoryHints = new ArrayList<>(memories.size());
        for (TurnMemory m : memories) {
            memoryHints.add(new TurnContext.L0MemorySummary(
                    m.turn(),
                    fallbackIntent(m.intent()),
                    Math.max(0, m.staminaLoss()),
                    m.rewardFlags() == null ? List.of() : m.rewardFlags(),
                    shorten(m.narration(), 48)
            ));

            String structuredText = "T" + m.turn()
                    + "|intent=" + fallbackIntent(m.intent())
                    + "|loss=" + Math.max(0, m.staminaLoss())
                    + "|gain=" + String.join(",", m.rewardFlags() == null ? List.of() : m.rewardFlags())
                    + "|note=" + shorten(m.narration(), 56);
            ctx.retrievedContexts.add(new TurnContext.RetrievedContext(
                    "memory_l0", "turn_" + m.turn(), structuredText, 0.68
            ));
        }
        ctx.rollingMemories = memoryHints;
    }

    private void injectEpisodicSummary(TurnContext ctx) {
        List<String> episodic = sessionRepo.findRecentEpisodicSummaries(ctx.sessionId, 2);
        if (episodic.isEmpty()) {
            return;
        }
        ctx.episodicSummaries = episodic;
        episodic.forEach(summary -> ctx.retrievedContexts.add(new TurnContext.RetrievedContext(
                "memory_l1", "episode", summary, 0.60
        )));
    }

    private void appendRollingMemory(TurnContext ctx) {
        String narration = ctx.finalNarration != null && !ctx.finalNarration.isBlank()
                ? ctx.finalNarration
                : (ctx.plot != null ? ctx.plot.text() : "");
        TurnMemory memory = new TurnMemory(
                ctx.session.getTurn(),
                ctx.playerInput,
            fallbackIntent(ctx.intent),
            resolveStaminaLoss(ctx),
            resolveRewardFlags(ctx),
                narration,
                Instant.now().toEpochMilli()
        );
        sessionRepo.appendTurnMemory(ctx.sessionId, memory);
        sessionRepo.appendEpisodicSummary(ctx.sessionId, buildEpisodicSummary(ctx, narration));
    }

    private boolean canFallback(List<String> violations) {
        return violations.stream().allMatch(v ->
                v.startsWith("OPTION_COUNT_VIOLATION") || v.startsWith("PLOT_EMPTY"));
    }

    private void applyFallback(TurnContext ctx) {
        ctx.options = List.of(
                new OptionPayload("opt_a", "快速突进并夺取掩体", "HIGH", "stamina:-8, risk:high"),
                new OptionPayload("opt_b", "保持潜行并观察敌情", "MEDIUM_LOW", "stamina:-2, risk:low"),
                new OptionPayload("opt_c", "优先搜集医疗与补给", "MEDIUM", "stamina:-4, loot:+"),
                new OptionPayload("opt_d", "转移至侧翼寻找退路", "MEDIUM", "stamina:-5, safety:+")
        );

        String base = (ctx.plot != null && ctx.plot.text() != null && !ctx.plot.text().isBlank())
                ? ctx.plot.text()
                : "你在混乱中迅速收缩行动半径，优先执行保守策略。";
        List<String> citations = ctx.plot != null ? ctx.plot.citations() : List.of();
        double confidence = ctx.plot != null ? Math.max(0.50, ctx.plot.confidence() - 0.15) : 0.50;
        ctx.plot = new PlotPayload(base + " 系统已切换到安全候选动作集。", citations, confidence);

        ctx.rulesPassed = true;
        ctx.extras.put("fallbackApplied", true);
        ctx.extras.put("fallbackViolations", new ArrayList<>(ctx.violations));
    }

    private String shorten(String text, int maxLen) {
        if (text == null || text.isBlank()) {
            return "无";
        }
        if (text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen - 1) + "…";
    }

    private String fallbackIntent(String intent) {
        return (intent == null || intent.isBlank()) ? "FREE_EXPLORE" : intent;
    }

    private int resolveStaminaLoss(TurnContext ctx) {
        if (ctx.stateDelta != null) {
            return Math.max(0, -ctx.stateDelta.stamina());
        }
        return 0;
    }

    private List<String> resolveRewardFlags(TurnContext ctx) {
        if (ctx.stateDelta == null || ctx.stateDelta.flagsAdded() == null) {
            return List.of();
        }
        return ctx.stateDelta.flagsAdded().stream()
                .filter(Objects::nonNull)
                .filter(flag -> !flag.isBlank())
                .limit(4)
                .toList();
    }

    private String buildEpisodicSummary(TurnContext ctx, String narration) {
        return "第" + ctx.session.getTurn()
                + "回合: 意图=" + fallbackIntent(ctx.intent)
                + "; 体力损耗=" + resolveStaminaLoss(ctx)
                + "; 收益=" + (resolveRewardFlags(ctx).isEmpty() ? "无" : String.join("/", resolveRewardFlags(ctx)))
                + "; 摘要=" + shorten(narration, 64);
    }

    private void saveTrace(TurnContext ctx, long t0, long elapsed, String status) {
        try {
            AgentMetricsStore.TraceDetail trace = new AgentMetricsStore.TraceDetail(
                    ctx.traceId,
                    ctx.sessionId,
                    ctx.session.getTurn(),
                    t0,
                    elapsed,
                    status,
                    new ArrayList<>(ctx.agentSpans)
            );
            metricsStore.saveTrace(trace);
        } catch (Exception e) {
            log.warn("[Orchestrator] saveTrace failed: {}", e.getMessage());
        }
    }
}
