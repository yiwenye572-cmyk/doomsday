package com.doomsday.game.agent.impl;

import com.doomsday.game.agent.AgentChain;
import com.doomsday.game.agent.AgentHandler;
import com.doomsday.game.agent.TurnContext;
import com.doomsday.game.api.OptionPayload;
import java.util.List;
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
        next.handle(ctx);
    }

    // ===== LLM 生成 =====

    private OptionsWrapper generateWithLlm(TurnContext ctx) {
        String userPrompt = """
                【当前位置】%s
                【玩家行动】%s
                【剧情摘要】%s
                【玩家状态】HP=%d 体力=%d 感染=%d
                
                请生成 4 个选项 JSON：
                """.formatted(
                ctx.session.getLocation(),
                ctx.playerInput,
                ctx.plot != null ? shorten(ctx.plot.text(), 120) : "（无）",
                ctx.session.getHp(),
                ctx.session.getStamina(),
                ctx.session.getInfection()
        );

        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(userPrompt)
                .call()
                .entity(OptionsWrapper.class);
    }

    // ===== 静态降级 =====

    private List<OptionPayload> staticFallback(TurnContext ctx) {
        String loc = ctx.session != null ? ctx.session.getLocation() : "unknown";
        return List.of(
                new OptionPayload("opt_a", "快速突进夺取掩体，以最短路线压制威胁", "HIGH", "体力:-8，风险高，可能获得先机"),
                new OptionPayload("opt_b", "保持低姿态潜行，观察环境再行动", "MEDIUM_LOW", "体力:-2，推进慢，遭遇率降低"),
                new OptionPayload("opt_c", "优先搜集附近可用医疗与补给", "MEDIUM", "体力:-4，补给+，无战斗"),
                new OptionPayload("opt_d", "绕行侧翼，寻找隐蔽入口或退路", "MEDIUM", "体力:-5，可能触发支线，也可能遇伏")
        );
    }

    private String shorten(String text, int max) {
        if (text == null || text.length() <= max) return text;
        return text.substring(0, max - 1) + "…";
    }

    /** Spring AI BeanOutputConverter 反序列化目标 */
    record OptionItem(String id, String text, String riskLevel, String expectedEffect) {}
    record OptionsWrapper(List<OptionItem> options) {}
}

