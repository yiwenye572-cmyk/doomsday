package com.doomsday.game.agent;

import com.doomsday.game.api.DifficultyDeltaPayload;
import com.doomsday.game.api.OptionPayload;
import com.doomsday.game.api.PlotPayload;
import com.doomsday.game.api.StateDeltaPayload;
import com.doomsday.game.admin.AgentMetricsStore;
import com.doomsday.game.tool.dto.ToolCallResult;
import com.doomsday.game.domain.GameSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 单回合责任链上下文——在各 Agent 节点间传递并累积结果。
 *
 * 约定：
 * - 各 Agent 只写自己负责的字段，不覆盖上游结果。
 * - aborted=true 时链路短路，后续节点不执行。
 */
public class TurnContext {

    // ===== 输入 =====
    public final String sessionId;
    public final GameSession session;
    public final String playerInput;
    public final String idempotencyKey;
    public final String traceId;

    // ===== Router Agent 输出 =====
    public String intent;              // EVENT_ADVANCE / RULE_QUERY / COMBAT / FREE_EXPLORE
    public double intentConfidence;
    public List<String> nextAgents = new ArrayList<>();

    // ===== Retrieval Agent 输出 =====
    public List<RetrievedContext> retrievedContexts = new ArrayList<>();

    // ===== L0 Rolling Memory（Redis） =====
    public List<L0MemorySummary> rollingMemories = new ArrayList<>();

    // ===== L1 Episodic Summary（Redis） =====
    public List<String> episodicSummaries = new ArrayList<>();

    // ===== Difficulty Director Agent 输出 =====
    public DifficultyDeltaPayload difficultyDelta;

    // ===== Plot Generation Agent 输出 =====
    public PlotPayload plot;

    // ===== Option Generation Agent 输出 =====
    public List<OptionPayload> options = new ArrayList<>();

    // ===== Rule Guard Agent 输出 =====
    public boolean rulesPassed = true;
    public List<String> violations = new ArrayList<>();

    // ===== State Commit Agent 输出 =====
    public StateDeltaPayload stateDelta;

    // ===== Narration Agent 输出 =====
    public String finalNarration;

    // ===== 链路控制 =====
    public boolean aborted = false;
    public String abortReason;
    public final Map<String, Object> extras = new HashMap<>();

    // ===== 可观测性：AOP 切面写入各 Agent 分段耗时 =====
    public List<AgentMetricsStore.AgentSpan> agentSpans = new ArrayList<>();

    // ===== ReAct：工具调用结果（按发生顺序） =====
    public List<ToolCallResult> toolCallResults = new ArrayList<>();

    // ===== 可观测性：LLM 分段与 Token 统计（由 Agent 内部写入） =====
    public final Map<String, LlmMetricAgg> llmMetrics = new HashMap<>();

    public TurnContext(String sessionId, GameSession session,
                       String playerInput, String idempotencyKey, String traceId) {
        this.sessionId = sessionId;
        this.session = session;
        this.playerInput = playerInput;
        this.idempotencyKey = idempotencyKey;
        this.traceId = traceId;
    }

    public void abort(String reason) {
        this.aborted = true;
        this.abortReason = reason;
    }

    /**
     * 聚合单个 Agent 内部的 LLM 调用指标。
     * 允许同一 Agent 多次调用模型，最终在切面层统一汇总。
     */
    public void addLlmMetric(String agentName,
                             long modelMs,
                             int promptTokens,
                             int completionTokens,
                             int totalTokens,
                             String modelName) {
        if (agentName == null || agentName.isBlank()) {
            return;
        }
        LlmMetricAgg agg = llmMetrics.computeIfAbsent(agentName, ignored -> new LlmMetricAgg());
        agg.modelMs += Math.max(0, modelMs);
        agg.promptTokens += Math.max(0, promptTokens);
        agg.completionTokens += Math.max(0, completionTokens);
        agg.totalTokens += Math.max(0, totalTokens);
        agg.calls += 1;
        if (modelName != null && !modelName.isBlank()) {
            agg.modelName = modelName;
        }
    }

    public LlmMetricAgg llmMetric(String agentName) {
        return llmMetrics.get(agentName);
    }

    public void addToolResult(ToolCallResult result) {
        if (result != null) {
            toolCallResults.add(result);
        }
    }

    public List<String> latestToolObservations(int maxItems) {
        int limit = Math.max(1, maxItems);
        if (toolCallResults.isEmpty()) {
            return List.of();
        }
        return toolCallResults.stream()
                .filter(ToolCallResult::success)
                .sorted((a, b) -> Long.compare(b.latencyMs(), a.latencyMs()))
                .limit(limit)
                .map(r -> r.toolName() + " => " + safeResultSummary(r.result()))
                .toList();
    }

    private static String safeResultSummary(Map<String, Object> result) {
        if (result == null || result.isEmpty()) {
            return "empty";
        }
        String value = result.toString();
        if (value.length() <= 120) {
            return value;
        }
        return value.substring(0, 119) + "...";
    }

    /** 检索结果条目 */
    public record RetrievedContext(
            String source,   // event_card / lorebook / memory_l0 / rule
            String id,
            String text,
            double score
    ) {}

        /** L0 结构化摘要（意图、损耗、收益）。 */
        public record L0MemorySummary(
            int turn,
            String intent,
            int staminaLoss,
            List<String> rewards,
            String narrationSnippet
        ) {}

    public static class LlmMetricAgg {
        public long modelMs;
        public int promptTokens;
        public int completionTokens;
        public int totalTokens;
        public int calls;
        public String modelName;
    }
}
