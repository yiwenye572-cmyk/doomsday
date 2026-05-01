package com.doomsday.game.agent.impl;

import com.doomsday.game.agent.AgentChain;
import com.doomsday.game.agent.AgentHandler;
import com.doomsday.game.agent.TurnContext;
import com.doomsday.game.agent.TurnContext.RetrievedContext;
import com.doomsday.game.api.PlotPayload;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Plot Generation Agent：在规则约束下生成本回合叙事文本。
 *
 * P1 实现：模板叙事 + 召回证据拼接引用。
 * 后续接入 LLM 时只替换 generateNarration 方法，接口不变。
 */
@Component
public class PlotGenerationAgent implements AgentHandler {

    private static final Logger log = LoggerFactory.getLogger(PlotGenerationAgent.class);

    @Override
    public String name() {
        return "PlotGenerationAgent";
    }

    @Override
    public void handle(TurnContext ctx, AgentChain next) {
        List<String> citations = ctx.retrievedContexts.stream()
                .map(rc -> rc.source() + ":" + rc.id())
                .limit(5)
                .toList();

        double confidence = ctx.retrievedContexts.isEmpty() ? 0.60
                : ctx.retrievedContexts.stream().mapToDouble(RetrievedContext::score).average().orElse(0.70);

        String narration = generateNarration(
            ctx.playerInput,
            ctx.session.getLocation(),
            ctx.intent,
            ctx.rollingMemories
        );
        ctx.plot = new PlotPayload(narration, citations, confidence);

        log.debug("[{}] traceId={} confidence={} citations={}",
                name(), ctx.traceId, String.format("%.2f", confidence), citations.size());

        next.handle(ctx);
    }

    private String generateNarration(String playerInput, String location, String intent, List<String> rollingMemories) {
        String envDesc = switch (location) {
            case "safe_house"     -> "破旧的避难所内弥漫着汽油与铁锈的气味，窗缝透进稀薄的灰色天光";
            case "old_gas_station"-> "废弃加油站的油漆早已剥落，停滞的空气里混着腐败物与机油的腥气";
            case "subway_ruins"   -> "地铁废墟深处，远处偶尔传来金属滚动的回响，黑暗沉甸甸地压着你";
            default               -> "末日的废土上，时间似乎已经失去了意义";
        };

        String memoryHint = "";
        if (rollingMemories != null && !rollingMemories.isEmpty()) {
            String joined = rollingMemories.stream().limit(2).reduce((a, b) -> a + "；" + b).orElse("");
            memoryHint = "你回想起最近的行动轨迹：" + joined + "。";
        }

        String actionFeedback = switch (intent) {
            case "COMBAT"        -> "你握紧了手中的武器，肌肉在绷紧的一瞬间感到一阵酸痛";
            case "FREE_EXPLORE"  -> "你放轻脚步，贴着墙壁向前摸索，尽量不发出任何声响";
            case "RULE_QUERY"    -> "你在脑海中快速整理着已知的规则与技能，试图找到最优解";
            default              -> "你压低呼吸，按照心中的计划一步步推进——" + playerInput;
        };

        return envDesc + "。" + memoryHint + actionFeedback + "。"
                + "体内残存的体力像沙漏里最后几粒沙，在每一次呼吸间悄悄流失。"
                + "远处有细微的响动，像是某种东西正在逼近，但你无暇顾及，"
                + "眼前的局面已经容不下任何犹豫——你必须立刻做出选择。";
    }
}
