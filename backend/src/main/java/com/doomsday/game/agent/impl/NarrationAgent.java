package com.doomsday.game.agent.impl;

import com.doomsday.game.agent.AgentChain;
import com.doomsday.game.agent.AgentHandler;
import com.doomsday.game.agent.TurnContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Narration Agent：对最终文案做风格统一，强制第二人称"你"，不改变事实状态。
 * P1 实现：确保文本以"你"开头；后续接入大模型润色时只替换 refine 方法。
 */
@Component
public class NarrationAgent implements AgentHandler {

    private static final Logger log = LoggerFactory.getLogger(NarrationAgent.class);

    @Override
    public String name() {
        return "NarrationAgent";
    }

    @Override
    public void handle(TurnContext ctx, AgentChain next) {
        String raw = ctx.plot != null ? ctx.plot.text() : "";
        ctx.finalNarration = refine(raw);
        log.debug("[{}] traceId={} length={}", name(), ctx.traceId, ctx.finalNarration.length());
        next.handle(ctx);
    }

    private String refine(String text) {
        if (text == null || text.isBlank()) {
            return "你凝视着前方的废墟，等待着命运的下一步安排。";
        }
        // 确保以第二人称"你"开头
        if (!text.startsWith("你")) {
            text = "你" + text;
        }
        // 若文本超过 500 字则截断（防止模板拼接越界）
        if (text.length() > 500) {
            text = text.substring(0, 497) + "……";
        }
        return text;
    }
}
