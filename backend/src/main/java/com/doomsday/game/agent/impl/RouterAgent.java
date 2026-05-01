package com.doomsday.game.agent.impl;

import com.doomsday.game.agent.AgentChain;
import com.doomsday.game.agent.AgentHandler;
import com.doomsday.game.agent.TurnContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Router Agent：识别玩家输入意图，决定后续链路。
 *
 * P1 实现为规则版：关键词匹配 + 默认意图 EVENT_ADVANCE。
 * 后续可替换为轻量模型调用，接口不变。
 */
@Component
public class RouterAgent implements AgentHandler {

    private static final Logger log = LoggerFactory.getLogger(RouterAgent.class);

    @Override
    public String name() {
        return "RouterAgent";
    }

    @Override
    public void handle(TurnContext ctx, AgentChain next) {
        String input = ctx.playerInput == null ? "" : ctx.playerInput.toLowerCase();

        if (input.contains("攻击") || input.contains("战斗") || input.contains("开枪")) {
            ctx.intent = "COMBAT";
            ctx.intentConfidence = 0.90;
        } else if (input.contains("规则") || input.contains("技能") || input.contains("查")) {
            ctx.intent = "RULE_QUERY";
            ctx.intentConfidence = 0.85;
        } else if (input.contains("探索") || input.contains("侦察") || input.contains("搜寻")) {
            ctx.intent = "FREE_EXPLORE";
            ctx.intentConfidence = 0.80;
        } else {
            ctx.intent = "EVENT_ADVANCE";
            ctx.intentConfidence = 0.75;
        }

        log.debug("[{}] traceId={} intent={} confidence={}",
                name(), ctx.traceId, ctx.intent, ctx.intentConfidence);

        next.handle(ctx);
    }
}
