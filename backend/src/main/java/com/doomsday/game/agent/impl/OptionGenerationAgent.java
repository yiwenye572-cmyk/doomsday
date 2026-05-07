package com.doomsday.game.agent.impl;

import com.doomsday.game.agent.AgentChain;
import com.doomsday.game.agent.AgentHandler;
import com.doomsday.game.agent.TurnContext;
import com.doomsday.game.api.OptionPayload;
import com.doomsday.game.common.LlmTokenEstimator;
import com.doomsday.game.tool.dto.ToolCallRequest;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * Option Generation Agent：调用 LLM（qwen-plus）生成本回合恰好 4 个可选行动。
 *
 * 固定策略框架：激进 / 稳健 / 资源导向 / 探索导向。
 * 降级：LLM 调用失败或返回非 4 选项时，回退到静态候选集。
 */
@Component
public class OptionGenerationAgent implements AgentHandler {

    private static final Logger log = LoggerFactory.getLogger(OptionGenerationAgent.class);

    private static final String SYSTEM_PROMPT = """
            你是一个末日生存游戏的选项设计师。
            根据当前剧情生成恰好 4 个可选行动，用 JSON 返回（不要 markdown 代码块）。
            
            每个选项格式（严格 JSON）：
            {"id":"opt_a","text":"...（15-40字）","riskLevel":"HIGH|MEDIUM|MEDIUM_LOW|LOW","expectedEffect":"...（简短描述）"}
            
            4 个选项必须覆盖：
            - opt_a：激进高风险行动
            - opt_b：稳健低风险行动
            - opt_c：资源/补给导向行动
            - opt_d：探索/侧翼行动
            
            返回格式：
            {"options":[...4个选项...]}
            """;

    private final ChatClient chatClient;

    public OptionGenerationAgent(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public String name() {
        return "OptionGenerationAgent";
    }

    @Override
    public void handle(TurnContext ctx, AgentChain next) {
        try {
            OptionsWrapper result = generateWithLlm(ctx);
            if (result != null && result.options() != null && result.options().size() == 4) {
                ctx.options = result.options().stream()
                        .map(o -> new OptionPayload(o.id(), o.text(), o.riskLevel(), o.expectedEffect()))
                        .toList();
            ctx.options = diversifyAgainstPrevious(ctx, ctx.options);
                log.debug("[{}] traceId={} options=4 (llm)", name(), ctx.traceId);
            } else {
                log.warn("[{}] traceId={} llm returned {} options, fallback",
                        name(), ctx.traceId, result == null ? 0 : (result.options() == null ? 0 : result.options().size()));
                ctx.options = staticFallback(ctx);
            }
        } catch (Exception e) {
            log.warn("[{}] traceId={} llm failed, fallback: {}", name(), ctx.traceId, e.getMessage());
            ctx.options = staticFallback(ctx);
        }
        ctx.options = diversifyAgainstPrevious(ctx, ctx.options);
        next.handle(ctx);
    }

    public List<ToolCallRequest> planToolCalls(TurnContext ctx) {
        try {
            ToolPlan plan = chatClient.prompt()
                    .user(buildToolPlanPrompt(ctx))
                    .call()
                    .entity(ToolPlan.class);
            List<ToolCallRequest> llmPlanned = plan == null || plan.toolCalls() == null
                ? List.of()
                : plan.toolCalls().stream()
                    .limit(2)
                    .filter(c -> c.toolName() != null && !c.toolName().isBlank())
                    .map(c -> new ToolCallRequest(
                            "tool_" + ctx.traceId + "_option_" + c.toolName(),
                            ctx.traceId,
                            name(),
                            c.toolName(),
                            1200,
                            c.payload() == null ? Map.of() : c.payload()
                    ))
                    .toList();
            return mergePreferDistinct(llmPlanned, heuristicPlan(ctx));
        } catch (Exception ex) {
            return heuristicPlan(ctx);
        }
    }

    // ===== LLM 生成 =====

    private OptionsWrapper generateWithLlm(TurnContext ctx) {
        String toolObs = ctx.latestToolObservations(3).isEmpty()
            ? "无"
            : String.join("; ", ctx.latestToolObservations(3));
        String rollingMemory = ctx.rollingMemories.isEmpty()
            ? "无"
            : ctx.rollingMemories.stream()
                .map(m -> "T" + m.turn() + ":" + m.intent() + "/损耗" + m.staminaLoss() + "/" + m.narrationSnippet())
                .limit(3)
                .reduce((a, b) -> a + "; " + b)
                .orElse("无");
        String episodic = ctx.episodicSummaries.isEmpty()
            ? "无"
            : ctx.episodicSummaries.stream().limit(2).reduce((a, b) -> a + " | " + b).orElse("无");
        String userPrompt = """
                【当前位置】%s
                【玩家行动】%s
                【剧情摘要】%s
                【玩家状态】HP=%d 体力=%d 感染=%d
                【背包】%s
                【最近记忆】%s
                【阶段摘要】%s
                【工具观察】%s
                
                请生成 4 个选项 JSON：
                """.formatted(
                ctx.session.getLocation(),
                ctx.playerInput,
                ctx.plot != null ? shorten(ctx.plot.text(), 120) : "（无）",
                ctx.session.getHp(),
                ctx.session.getStamina(),
                ctx.session.getInfection(),
                ctx.session.getInventory(),
                rollingMemory,
                episodic,
                toolObs
        );

            long llmStart = System.nanoTime();
            OptionsWrapper wrapper = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(userPrompt)
                .call()
                .entity(OptionsWrapper.class);
            long modelMs = (System.nanoTime() - llmStart) / 1_000_000;

            String outputSnapshot = wrapper == null || wrapper.options() == null
                ? "{}"
                : "{\"options\":" + wrapper.options().size() + "}";
            int promptTokens = LlmTokenEstimator.estimatePromptTokens(SYSTEM_PROMPT + "\n" + userPrompt);
            int completionTokens = LlmTokenEstimator.estimateCompletionTokens(outputSnapshot);
            int totalTokens = promptTokens + completionTokens;
            ctx.addLlmMetric(name(), modelMs, promptTokens, completionTokens, totalTokens, "qwen-plus");
            return wrapper;
    }

    // ===== 静态降级 =====

    private List<OptionPayload> staticFallback(TurnContext ctx) {
        String loc = humanizeLocation(ctx.session != null ? ctx.session.getLocation() : "unknown");
        String motif = extractMotif(ctx.plot == null ? "" : ctx.plot.text());
        boolean lowStamina = ctx.session != null && ctx.session.getStamina() <= 35;
        String cautiousVerb = lowStamina ? "短暂停步恢复呼吸" : "压低身形持续观察";
        String resourceHint = ctx.session != null && ctx.session.getInventory().contains("bandage")
                ? "整理现有物资并补充缺口"
                : "优先搜寻药品和食物";
        return List.of(
                new OptionPayload("opt_a", "趁" + motif + "强行推进到" + loc + "深处", "HIGH", "体力:-8，可能快速突破，也可能正面遭遇威胁"),
                new OptionPayload("opt_b", cautiousVerb + "，先确认" + loc + "周边动静", "MEDIUM_LOW", "体力:-2，推进更稳，暴露概率下降"),
                new OptionPayload("opt_c", resourceHint + "，在" + loc + "边缘做一次补给搜索", "MEDIUM", "体力:-4，补给收益更高，但节奏放缓"),
                new OptionPayload("opt_d", "沿" + loc + "侧翼绕行，寻找新的线索或退路", "MEDIUM", "体力:-5，可能触发支线，也可能发现隐藏入口")
        );
    }

    private List<OptionPayload> diversifyAgainstPrevious(TurnContext ctx, List<OptionPayload> current) {
        if (current == null || current.isEmpty() || ctx.session == null || ctx.session.getCurrentOptions() == null) {
            return current;
        }
        List<OptionPayload> previous = ctx.session.getCurrentOptions();
        if (previous.isEmpty()) {
            return current;
        }

        String suffix = buildVariationSuffix(ctx);
        return current.stream().map(option -> {
            OptionPayload prev = previous.stream()
                    .filter(item -> item.id().equals(option.id()))
                    .findFirst()
                    .orElse(null);
            if (prev == null || !prev.text().equals(option.text())) {
                return option;
            }
            return new OptionPayload(
                    option.id(),
                    option.text() + suffix,
                    option.riskLevel(),
                    option.expectedEffect()
            );
        }).toList();
    }

    private String buildVariationSuffix(TurnContext ctx) {
        String input = ctx.playerInput == null ? "" : ctx.playerInput;
        if (input.contains("强行") || input.contains("突进") || input.contains("冲")) {
            return "，趁余势未散";
        }
        if (input.contains("观察") || input.contains("潜行")) {
            return "，借下一次空档执行";
        }
        if (input.contains("补给") || input.contains("搜集")) {
            return "，优先处理新暴露的资源点";
        }
        return "，应对第" + (ctx.session.getTurn() + 1) + "回合新局面";
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
            return "雨幕掩护";
        }
        if (plotText.contains("灯") || plotText.contains("光")) {
            return "微弱光线";
        }
        if (plotText.contains("血") || plotText.contains("拖拽")) {
            return "残留痕迹";
        }
        return shorten(plotText, 8);
    }

    private String shorten(String text, int max) {
        if (text == null || text.length() <= max) return text;
        return text.substring(0, max - 1) + "…";
    }

    private String buildToolPlanPrompt(TurnContext ctx) {
        return """
                你是 ReAct 工具规划器。请判断生成选项前是否需要工具调用。
                可用工具：WorldQueryTool, MemoryRecallTool, EntityStatePatchTool。
                仅返回 JSON，最多 2 个工具。
                若调用 EntityStatePatchTool 必须带 dryRun=true。

                输入：%s
                意图：%s
                剧情摘要：%s

                返回：
                {"toolCalls":[{"toolName":"EntityStatePatchTool","payload":{"dryRun":true,"staminaDelta":-2}}]}
                """.formatted(
                ctx.playerInput,
                ctx.intent,
                ctx.plot == null ? "无" : shorten(ctx.plot.text(), 90)
        );
    }

    private List<ToolCallRequest> heuristicPlan(TurnContext ctx) {
        String intent = ctx.intent == null ? "" : ctx.intent;
        if ("COMBAT".equals(intent)) {
            return List.of(new ToolCallRequest(
                    "tool_" + ctx.traceId + "_option_patch",
                    ctx.traceId,
                    name(),
                    "EntityStatePatchTool",
                    1200,
                    Map.of("dryRun", true, "staminaDelta", -6)
            ));
        }
        if ("RULE_QUERY".equals(intent)) {
            return List.of(new ToolCallRequest(
                    "tool_" + ctx.traceId + "_option_memory",
                    ctx.traceId,
                    name(),
                    "MemoryRecallTool",
                    1200,
                    Map.of("limit", 3, "includeEpisodic", true)
            ));
        }
        return List.of();
    }

    private List<ToolCallRequest> mergePreferDistinct(List<ToolCallRequest> primary, List<ToolCallRequest> fallback) {
        List<ToolCallRequest> merged = new java.util.ArrayList<>();
        if (primary != null) {
            merged.addAll(primary);
        }
        if (fallback != null) {
            for (ToolCallRequest item : fallback) {
                boolean exists = merged.stream().anyMatch(x -> x.toolName().equals(item.toolName()));
                if (!exists) {
                    merged.add(item);
                }
            }
        }
        return merged.stream().limit(2).toList();
    }

    /** Spring AI BeanOutputConverter 反序列化目标 */
    record OptionItem(String id, String text, String riskLevel, String expectedEffect) {}
    record OptionsWrapper(List<OptionItem> options) {}
    record ToolPlanCall(String toolName, Map<String, Object> payload) {}
    record ToolPlan(List<ToolPlanCall> toolCalls) {}
}

