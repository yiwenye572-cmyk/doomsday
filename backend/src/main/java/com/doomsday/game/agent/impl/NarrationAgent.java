package com.doomsday.game.agent.impl;

import com.doomsday.game.agent.AgentChain;
import com.doomsday.game.agent.AgentHandler;
import com.doomsday.game.agent.TurnContext;
import com.doomsday.game.api.PlotPayload;
import com.doomsday.game.common.LlmTokenEstimator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Narration Agent：文风统一润色。
 *
 * 调用 qwen-turbo（低延迟）对 PlotGenerationAgent 输出进行风格校正：
 *   - 保持第二人称"你"
 *   - 不超 500 字
 *   - 不增删事实，只做文风微调
 *
 * 降级：LLM 调用失败时保留原剧情文本，保证链路不中断。
 */
@Component
public class NarrationAgent implements AgentHandler {

    private static final Logger log = LoggerFactory.getLogger(NarrationAgent.class);

    private static final int MAX_CHARS = 500;

    private static final String SYSTEM_PROMPT = """
            你是一个末日文学风格编辑助手，只做文风润色，不增删任何事实信息。
            规则：
            1. 统一使用第二人称"你"。
            2. 不超过 500 字。
            3. 保留原文所有事件、地点、道具信息。
            4. 文风：压抑、颓废、感官细节丰富，偏向末日求生小说。
            5. 只输出润色后的文本，不要解释或添加前缀。
            """;

    private final ChatClient chatClient;

    public NarrationAgent(@Qualifier("routerChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public String name() {
        return "NarrationAgent";
    }

    @Override
    public void handle(TurnContext ctx, AgentChain next) {
        if (ctx.plot == null || ctx.plot.text() == null || ctx.plot.text().isBlank()) {
            ctx.finalNarration = "你凝视着前方的废墟，等待着命运的下一步安排。";
            next.handle(ctx);
            return;
        }

        String raw = ctx.plot.text();
        String polished;
        try {
            long llmStart = System.nanoTime();
            polished = polish(raw);
            long modelMs = (System.nanoTime() - llmStart) / 1_000_000;
            String promptText = SYSTEM_PROMPT + "\n请润色以下剧情文本：\n\n" + raw;
            int promptTokens = LlmTokenEstimator.estimatePromptTokens(promptText);
            int completionTokens = LlmTokenEstimator.estimateCompletionTokens(polished);
            int totalTokens = promptTokens + completionTokens;
            ctx.addLlmMetric(name(), modelMs, promptTokens, completionTokens, totalTokens, "qwen-turbo");
            polished = truncate(polished, MAX_CHARS);
            log.debug("[{}] traceId={} polished {} → {} chars", name(), ctx.traceId, raw.length(), polished.length());
        } catch (Exception e) {
            log.warn("[{}] traceId={} llm failed, keeping original: {}", name(), ctx.traceId, e.getMessage());
            polished = truncate(raw, MAX_CHARS);
        }

        ctx.finalNarration = polished;
        ctx.plot = new PlotPayload(polished, ctx.plot.citations(), ctx.plot.confidence());
        next.handle(ctx);
    }

    private String polish(String raw) {
        String content = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user("请润色以下剧情文本：\n\n" + raw)
                .call()
                .content();
        return content == null ? raw : content.strip();
    }

    private String truncate(String text, int max) {
        if (text == null) return "你凝视着前方的废墟，等待着命运的下一步安排。";
        if (text.length() <= max) return text;
        // 在 max 之前找最后一个中文句号/叹号/问号断句
        int cut = max;
        for (int i = max - 1; i > max - 30 && i > 0; i--) {
            char c = text.charAt(i);
            if (c == '。' || c == '！' || c == '？') {
                cut = i + 1;
                break;
            }
        }
        return text.substring(0, cut);
    }
}

