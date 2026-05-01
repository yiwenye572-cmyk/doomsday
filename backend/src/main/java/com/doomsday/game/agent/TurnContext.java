package com.doomsday.game.agent;

import com.doomsday.game.api.DifficultyDeltaPayload;
import com.doomsday.game.api.OptionPayload;
import com.doomsday.game.api.PlotPayload;
import com.doomsday.game.api.StateDeltaPayload;
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
    public List<String> rollingMemories = new ArrayList<>();

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

    /** 检索结果条目 */
    public record RetrievedContext(
            String source,   // event_card / lorebook / memory_l0 / rule
            String id,
            String text,
            double score
    ) {}
}
