package com.doomsday.game.agent.impl;

import com.doomsday.game.agent.AgentChain;
import com.doomsday.game.agent.AgentHandler;
import com.doomsday.game.agent.TurnContext;
import com.doomsday.game.common.LlmTokenEstimator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Router Agent：调用 LLM（qwen-turbo）对玩家输入进行意图分类。
 *
 * 输出意图：
 *   COMBAT        — 战斗/对抗行动
 *   FREE_EXPLORE  — 自由探索/搜刮
 *   EVENT_ADVANCE — 剧情/任务推进
 *   RULE_QUERY    — 规则/数值查询
 *
 * 降级：LLM 调用失败时回退到关键词匹配，保证链路不中断。
 */
@Component
public class RouterAgent implements AgentHandler {

    private static final Logger log = LoggerFactory.getLogger(RouterAgent.class);

    private final ChatClient chatClient;

    public RouterAgent(@Qualifier("routerChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public String name() {
        return "RouterAgent";
    }

    @Override
    public void handle(TurnContext ctx, AgentChain next) {
        try {
            String prompt = buildClassifyPrompt(ctx.playerInput, ctx.session.getLocation());
            long llmStart = System.nanoTime();
            RouterOutput result = classifyWithLlm(prompt);
            long modelMs = (System.nanoTime() - llmStart) / 1_000_000;
            ctx.intent = normalizeIntent(result.intent());
            ctx.intentConfidence = clamp(result.confidence(), 0.0, 1.0);
            String outputSnapshot = "{\"intent\":\"" + ctx.intent + "\",\"confidence\":" + ctx.intentConfidence + "}";
            int promptTokens = LlmTokenEstimator.estimatePromptTokens(prompt);
            int completionTokens = LlmTokenEstimator.estimateCompletionTokens(outputSnapshot);
            int totalTokens = promptTokens + completionTokens;
            ctx.addLlmMetric(name(), modelMs, promptTokens, completionTokens, totalTokens, "qwen-turbo");
            log.debug("[{}] traceId={} intent={} confidence={} (llm)",
                    name(), ctx.traceId, ctx.intent, ctx.intentConfidence);
        } catch (Exception e) {
            log.warn("[{}] traceId={} llm failed, fallback to keyword matching: {}",
                    name(), ctx.traceId, e.getMessage());
            keywordFallback(ctx);
        }
        next.handle(ctx);
    }

    // ===== LLM 分类 =====

    private String buildClassifyPrompt(String playerInput, String location) {
        return """
                你是一个游戏意图分类器，只返回 JSON，不要代码块或多余文字。
                
                玩家输入：%s
                当前位置：%s
                
                分类规则：
                - COMBAT：攻击/战斗/杀/反击/开枪
                - FREE_EXPLORE：搜刮/探索/搜索/查看/检查
                - EVENT_ADVANCE：推进剧情/对话/触发任务/前往
                - RULE_QUERY：询问规则/数值/技能效果
                
                返回格式（严格 JSON）：
                {"intent":"COMBAT","confidence":0.92}
                """.formatted(playerInput, location);
            }

            private RouterOutput classifyWithLlm(String prompt) {
        return chatClient.prompt()
                .user(prompt)
                .call()
                .entity(RouterOutput.class);
    }

    // ===== 关键词降级 =====

    private void keywordFallback(TurnContext ctx) {
        String input = ctx.playerInput == null ? "" : ctx.playerInput.toLowerCase();
        if (input.contains("攻击") || input.contains("战斗") || input.contains("开枪") || input.contains("杀")) {
            ctx.intent = "COMBAT";
            ctx.intentConfidence = 0.70;
        } else if (input.contains("搜刮") || input.contains("探索") || input.contains("检查") || input.contains("查找")) {
            ctx.intent = "FREE_EXPLORE";
            ctx.intentConfidence = 0.70;
        } else if (input.contains("规则") || input.contains("技能") || input.contains("效果") || input.contains("数值")) {
            ctx.intent = "RULE_QUERY";
            ctx.intentConfidence = 0.65;
        } else {
            ctx.intent = "EVENT_ADVANCE";
            ctx.intentConfidence = 0.60;
        }
    }

    private String normalizeIntent(String raw) {
        if (raw == null) return "EVENT_ADVANCE";
        return switch (raw.toUpperCase().strip()) {
            case "COMBAT" -> "COMBAT";
            case "FREE_EXPLORE" -> "FREE_EXPLORE";
            case "RULE_QUERY" -> "RULE_QUERY";
            default -> "EVENT_ADVANCE";
        };
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    /** Spring AI BeanOutputConverter 反序列化目标 */
    record RouterOutput(String intent, double confidence) {}
}

