package com.doomsday.game.agent.impl;

import com.doomsday.game.agent.AgentChain;
import com.doomsday.game.agent.AgentHandler;
import com.doomsday.game.agent.TurnContext;
import com.doomsday.game.api.OptionPayload;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Option Generation Agent：固定生成 4 个选项（激进/稳健/资源导向/探索导向）。
 * 后续可根据 playerProfile 动态生成，接口不变。
 */
@Component
public class OptionGenerationAgent implements AgentHandler {

    private static final Logger log = LoggerFactory.getLogger(OptionGenerationAgent.class);

    @Override
    public String name() {
        return "OptionGenerationAgent";
    }

    @Override
    public void handle(TurnContext ctx, AgentChain next) {
        ctx.options = generateOptions(ctx.intent, ctx.session.getLocation());
        log.debug("[{}] traceId={} options={}", name(), ctx.traceId, ctx.options.size());
        next.handle(ctx);
    }

    private List<OptionPayload> generateOptions(String intent, String location) {
        // P1：固定 4 选项，后续接入 LLM 时根据剧情动态生成
        return List.of(
                new OptionPayload("opt_a",
                        buildAggressiveOption(intent),
                        "HIGH",
                        "可能获得稀有资源或情报，但噪音与威胁风险显著上升"),
                new OptionPayload("opt_b",
                        "原地压低身形，观察周围动静后谨慎推进（稳健）",
                        "MEDIUM_LOW",
                        "降低遭遇概率，进展较慢但更安全"),
                new OptionPayload("opt_c",
                        "就地收集可用物资，优先补充消耗（资源导向）",
                        "MEDIUM",
                        "短期收益一般，但提升后续生存容错"),
                new OptionPayload("opt_d",
                        "绕行侧翼寻找隐蔽路线（探索导向）",
                        "MEDIUM_HIGH",
                        "可能触发支线与隐藏线索，也可能遭遇伏击")
        );
    }

    private String buildAggressiveOption(String intent) {
        return switch (intent) {
            case "COMBAT"       -> "立即发动先手攻击，争取快速解决威胁（激进）";
            case "FREE_EXPLORE" -> "快速突入目标区域，强行搜刮后撤离（激进）";
            default             -> "以最快速度完成目标，不惜一切代价（激进）";
        };
    }
}
