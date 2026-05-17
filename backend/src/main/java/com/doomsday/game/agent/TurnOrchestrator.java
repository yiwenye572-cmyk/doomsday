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
import com.doomsday.game.diary.GameDiaryService;
import com.doomsday.game.domain.GameSession;
import com.doomsday.game.domain.GameTimeFlow;
import com.doomsday.game.domain.SessionRepository;
import com.doomsday.game.domain.TurnMemory;
import com.doomsday.game.tool.ToolContext;
import com.doomsday.game.tool.ToolExecutor;
import com.doomsday.game.tool.dto.ToolCallRequest;
import com.doomsday.game.tool.dto.ToolCallResult;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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
    private final List<AgentHandler> preReActPipeline;
    private final List<AgentHandler> postPlotPipeline;
    private final List<AgentHandler> postCommitPipeline;
    private final AgentMetricsStore metricsStore;
    private final ConflictArbitrator conflictArbitrator;
    private final GameDiaryService gameDiaryService;
    private final PlotGenerationAgent plotAgent;
    private final OptionGenerationAgent optionAgent;
    private final ToolExecutor toolExecutor;

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
            ConflictArbitrator conflictArbitrator,
            GameDiaryService gameDiaryService,
            ToolExecutor toolExecutor) {
        this.sessionRepo = sessionRepo;
        this.metricsStore = metricsStore;
        this.conflictArbitrator = conflictArbitrator;
        this.gameDiaryService = gameDiaryService;
        this.plotAgent = plot;
        this.optionAgent = option;
        this.toolExecutor = toolExecutor;
        this.preReActPipeline = List.of(router, retrieval, difficultyDirector);
        this.postPlotPipeline = List.of(option, ruleGuard);
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
        injectMemoryHints(ctx);

        log.info("[Orchestrator] START sessionId={} turn={} traceId={}", sessionId, session.getTurn() + 1, traceId);
        long t0 = System.currentTimeMillis();

        new AgentChain(preReActPipeline).handle(ctx);

        if (!ctx.aborted) {
            List<ToolCallRequest> plotRequests = new ArrayList<>(plotAgent.planToolCalls(ctx));
            if (shouldForceMemoryRecall(ctx)
                    && plotRequests.stream().noneMatch(r -> "MemoryRecallTool".equals(r.toolName()))) {
                plotRequests.add(new ToolCallRequest(
                        "tool_" + ctx.traceId + "_forced_memory",
                        ctx.traceId,
                        "TurnOrchestrator",
                        "MemoryRecallTool",
                        1200,
                        Map.of("limit", 4, "includeEpisodic", true)
                ));
            }
            executeToolCalls(ctx, plotRequests, "PlotPlanning");
            new AgentChain(List.of(plotAgent)).handle(ctx);
        }

        if (!ctx.aborted) {
            executeToolCalls(ctx, optionAgent.planToolCalls(ctx), "OptionPlanning");
            new AgentChain(postPlotPipeline).handle(ctx);
        }

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
        gameDiaryService.maybeSummarizeByTurn(ctx.sessionId);

        log.info("[Orchestrator] DONE traceId={} turn={} v={} elapsed={}ms",
                traceId, session.getTurn(), session.getVersion(), elapsed);
        saveTrace(ctx, t0, elapsed, "OK");
        return ctx;
    }

    private void appendRollingMemory(TurnContext ctx) {
        String narration = ctx.finalNarration != null && !ctx.finalNarration.isBlank()
                ? ctx.finalNarration
                : (ctx.plot != null ? ctx.plot.text() : "");
        TurnMemory memory = new TurnMemory(
                ctx.session.getTurn(),
            ctx.session.getDayIndex(),
            ctx.session.getTimePhase(),
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

    private void executeToolCalls(TurnContext ctx, List<ToolCallRequest> requests, String stage) {
        if (requests == null || requests.isEmpty()) {
            return;
        }
        List<ToolCallResult> results = toolExecutor.executeBatch(
                new ToolContext(ctx.sessionId, ctx.traceId, "TurnOrchestrator", ctx.session, ctx),
                requests,
                true
        );
        for (ToolCallResult result : results) {
            ctx.addToolResult(result);
            if (!result.success()) {
                log.warn("[Orchestrator] tool failed traceId={} stage={} tool={} error={}",
                        ctx.traceId,
                        stage,
                    result.toolName(),
                        result.errorMessage());
                continue;
            }
            String observation = formatObservation(result);
            ctx.retrievedContexts.add(new TurnContext.RetrievedContext(
                    "tool_obs",
                    result.toolName(),
                    observation,
                    0.72
            ));
        }
    }

    private String formatObservation(ToolCallResult result) {
        if (result.result() == null || result.result().isEmpty()) {
            return result.toolName() + " returned empty";
        }
        String raw = result.result().toString();
        if (raw.length() > 140) {
            raw = raw.substring(0, 139) + "...";
        }
        return result.toolName() + ": " + raw;
    }

    private boolean shouldForceMemoryRecall(TurnContext ctx) {
        String input = ctx.playerInput == null ? "" : ctx.playerInput;
        return input.contains("回忆") || input.contains("之前") || input.contains("上次");
    }

    private boolean canFallback(List<String> violations) {
        return violations.stream().allMatch(v ->
                v.startsWith("OPTION_COUNT_VIOLATION") || v.startsWith("PLOT_EMPTY"));
    }

    private void applyFallback(TurnContext ctx) {
        String loc = humanizeLocation(ctx.session.getLocation());
        String motif = extractMotif(ctx.plot == null ? "" : ctx.plot.text());
        boolean lowStamina = ctx.session.getStamina() <= 35;
        ctx.options = List.of(
            new OptionPayload("opt_a", "借" + motif + "强冲进" + loc + "更深处", "HIGH", "stamina:-8, risk:high"),
            new OptionPayload("opt_b", (lowStamina ? "先贴墙停步" : "继续低姿潜行") + "，观察" + loc + "敌情", "MEDIUM_LOW", "stamina:-2, risk:low"),
            new OptionPayload("opt_c", "沿" + loc + "边缘搜集医疗与补给", "MEDIUM", "stamina:-4, loot:+"),
            new OptionPayload("opt_d", "转向" + loc + "侧翼，寻找新的退路与线索", "MEDIUM", "stamina:-5, safety:+")
        );
        ctx.options = diversifyFallbackAgainstPrevious(ctx, ctx.options);

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

    private String humanizeLocation(String location) {
        if (location == null || location.isBlank()) {
            return "废墟区域";
        }
        return switch (location) {
            case "safe_house" -> "安全屋外围";
            case "old_gas_station" -> "废弃加油站";
            default -> location.replace('_', ' ');
        };
    }

    private String extractMotif(String plotText) {
        if (plotText == null || plotText.isBlank()) {
            return "混乱余波";
        }
        if (plotText.contains("雨")) {
            return "雨幕";
        }
        if (plotText.contains("灯") || plotText.contains("光")) {
            return "微光";
        }
        if (plotText.contains("血") || plotText.contains("拖拽")) {
            return "痕迹";
        }
        return shorten(plotText, 6);
    }

    private List<OptionPayload> diversifyFallbackAgainstPrevious(TurnContext ctx, List<OptionPayload> current) {
        if (ctx.session == null || ctx.session.getCurrentOptions() == null || ctx.session.getCurrentOptions().isEmpty()) {
            return current;
        }
        String suffix = "，切换到第" + (ctx.session.getTurn() + 1) + "回合保守策略";
        return current.stream().map(option -> {
            OptionPayload previous = ctx.session.getCurrentOptions().stream()
                    .filter(item -> item.id().equals(option.id()))
                    .findFirst()
                    .orElse(null);
            if (previous == null || !previous.text().equals(option.text())) {
                return option;
            }
            return new OptionPayload(option.id(), option.text() + suffix, option.riskLevel(), option.expectedEffect());
        }).toList();
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
            + "回合(第" + ctx.session.getDayIndex() + "天 " + GameTimeFlow.phaseLabel(ctx.session.getTimePhase()) + ")"
            + ": 意图=" + fallbackIntent(ctx.intent)
                + "; 体力损耗=" + resolveStaminaLoss(ctx)
                + "; 收益=" + (resolveRewardFlags(ctx).isEmpty() ? "无" : String.join("/", resolveRewardFlags(ctx)))
                + "; 摘要=" + shorten(narration, 64);
    }

    private void saveTrace(TurnContext ctx, long t0, long elapsed, String status) {
        try {
            int eventHitCount = resolveEventHitCount(ctx);
            int eventCandidateCount = resolveEventCandidateCount(ctx);
            AgentMetricsStore.TraceDetail trace = new AgentMetricsStore.TraceDetail(
                    ctx.traceId,
                    ctx.sessionId,
                    ctx.session.getTurn(),
                    t0,
                    elapsed,
                    status,
                    new ArrayList<>(ctx.agentSpans),
                    resolveConflictDetected(ctx),
                    eventHitCount > 0,
                    eventHitCount,
                    eventCandidateCount,
                    extractDouble(ctx.extras.get("rag.vectorSimilarityMean")),
                    extractInt(ctx.extras.get("rag.vectorRetrievedCount"))
            );
            metricsStore.saveTrace(trace);
        } catch (Exception e) {
            log.warn("[Orchestrator] saveTrace failed: {}", e.getMessage());
        }
    }

    private void injectMemoryHints(TurnContext ctx) {
        CompletableFuture<List<TurnMemory>> rollingFuture = CompletableFuture.supplyAsync(
                () -> sessionRepo.findRecentTurnMemories(ctx.sessionId, 4)
        );
        CompletableFuture<List<String>> episodicFuture = CompletableFuture.supplyAsync(
                () -> sessionRepo.findRecentEpisodicSummaries(ctx.sessionId, 2)
        );

        List<TurnMemory> memories = rollingFuture.join();
        if (!memories.isEmpty()) {
            List<TurnContext.L0MemorySummary> memoryHints = new ArrayList<>(memories.size());
            for (TurnMemory m : memories) {
                memoryHints.add(new TurnContext.L0MemorySummary(
                        m.turn(),
                        fallbackIntent(m.intent()),
                        Math.max(0, m.staminaLoss()),
                        m.rewardFlags() == null ? List.of() : m.rewardFlags(),
                        "第" + Math.max(1, m.dayIndex()) + "天 "
                            + GameTimeFlow.phaseLabel(m.timePhase())
                            + " · " + shorten(m.narration(), 36)
                ));

                String structuredText = "D" + Math.max(1, m.dayIndex())
                        + "|" + GameTimeFlow.phaseLabel(m.timePhase())
                        + "|T" + m.turn()
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

        List<String> episodic = episodicFuture.join();
        if (!episodic.isEmpty()) {
            ctx.episodicSummaries = episodic;
            episodic.forEach(summary -> ctx.retrievedContexts.add(new TurnContext.RetrievedContext(
                    "memory_l1", "episode", summary, 0.60
            )));
        }
    }

    private boolean resolveConflictDetected(TurnContext ctx) {
        if (ctx.violations != null && !ctx.violations.isEmpty()) {
            return true;
        }
        Object arbitration = ctx.extras.get("arbitration");
        if (arbitration instanceof ArbitrationResult result && !result.pass()) {
            return true;
        }
        Object fallbackApplied = ctx.extras.get("fallbackApplied");
        return fallbackApplied instanceof Boolean v && v;
    }

    private int resolveEventHitCount(TurnContext ctx) {
        if (ctx.plot == null || ctx.plot.citations() == null) {
            return 0;
        }
        return (int) ctx.plot.citations().stream()
                .filter(Objects::nonNull)
                .filter(citation -> citation.startsWith("event_card:"))
                .count();
    }

    private int resolveEventCandidateCount(TurnContext ctx) {
        if (ctx.retrievedContexts == null) {
            return 0;
        }
        return (int) ctx.retrievedContexts.stream()
                .filter(Objects::nonNull)
                .filter(item -> "event_card".equalsIgnoreCase(item.source()))
                .count();
    }

    private double extractDouble(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        return 0.0;
    }

    private int extractInt(Object value) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        return 0;
    }
}
