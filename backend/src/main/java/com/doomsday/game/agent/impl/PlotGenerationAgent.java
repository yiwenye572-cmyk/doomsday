package com.doomsday.game.agent.impl;

import com.doomsday.game.agent.AgentChain;
import com.doomsday.game.agent.AgentHandler;
import com.doomsday.game.agent.TurnContext;
import com.doomsday.game.agent.TurnContext.RetrievedContext;
import com.doomsday.game.api.PlotPayload;
import com.doomsday.game.domain.GameSession;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * Plot Generation Agent：调用 LLM（qwen-plus）在规则约束下生成本回合叙事。
 *
 * 注入上下文：
 *   - 召回的事件卡、Lorebook 片段
 *   - L0 Rolling Memory（最近 N 回合轨迹）
 *   - 玩家当前状态快照
 *
 * 降级：LLM 调用失败时回退到模板叙事，链路不中断。
 */
@Component
public class PlotGenerationAgent implements AgentHandler {

    private static final Logger log = LoggerFactory.getLogger(PlotGenerationAgent.class);

    private static final String SYSTEM_PROMPT = """
            你是一个末日生存文字冒险游戏的叙事引擎。
            
            规则：
            1. 统一使用第二人称"你"叙事。
            2. 每次输出 220-420 字，不超过 450 字。
            3. 文风：末日求生小说，强调压迫感、资源稀缺感与感官细节。
            4. 叙事结构：环境描写 → 行动反馈 → 风险暗示 → 钩子收束。
            5. 不得虚构未在背景资料中定义的元素。
            6. 不要输出选项，只输出剧情文本。
            """;

    private final ChatClient chatClient;

    public PlotGenerationAgent(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public String name() {
        return "PlotGenerationAgent";
    }

    @Override
    public void handle(TurnContext ctx, AgentChain next) {
        List<String> citations = ctx.retrievedContexts.stream()
                .map(rc -> rc.source() + ":" + rc.id())
                .limit(6)
                .toList();
        double confidence = ctx.retrievedContexts.stream()
                .mapToDouble(RetrievedContext::score)
                .average()
                .orElse(0.65);

        String plotText;
        try {
            plotText = generateWithLlm(ctx);
            log.debug("[{}] traceId={} confidence={} citations={}",
                    name(), ctx.traceId, String.format("%.2f", confidence), citations.size());
        } catch (Exception e) {
            log.warn("[{}] traceId={} llm failed, fallback to template: {}",
                    name(), ctx.traceId, e.getMessage());
            plotText = templateFallback(ctx.playerInput, ctx.session.getLocation(), ctx.intent);
            confidence = 0.50;
        }

        ctx.plot = new PlotPayload(plotText, citations, confidence);
        next.handle(ctx);
    }

    // ===== LLM 叙事生成 =====

    private String generateWithLlm(TurnContext ctx) {
        String userPrompt = buildUserPrompt(ctx);
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(userPrompt)
                .call()
                .content();
    }

    private String buildUserPrompt(TurnContext ctx) {
        GameSession s = ctx.session;
        StringBuilder sb = new StringBuilder();

        // 状态快照
        sb.append("【玩家状态】\n");
        sb.append("位置: ").append(s.getLocation())
          .append(" | HP: ").append(s.getHp())
          .append(" | 体力: ").append(s.getStamina())
          .append(" | 感染: ").append(s.getInfection())
          .append(" | 回合: ").append(s.getTurn() + 1).append("\n\n");

        // L0 Rolling Memory
        if (!ctx.rollingMemories.isEmpty()) {
            sb.append("【最近行动轨迹（结构化摘要，参考后内化）】\n");
            ctx.rollingMemories.stream().limit(3).forEach(m -> sb.append("- T").append(m.turn())
                    .append(" | 意图=").append(m.intent())
                    .append(" | 损耗=").append(m.staminaLoss())
                    .append(" | 收益=").append(m.rewards().isEmpty() ? "无" : String.join("/", m.rewards()))
                    .append(" | 摘要=").append(m.narrationSnippet())
                    .append("\n"));
            sb.append("\n");
        }

        // L1 Episodic Memory
        if (!ctx.episodicSummaries.isEmpty()) {
            sb.append("【中期情节记忆（避免剧情断层）】\n");
            ctx.episodicSummaries.stream().limit(2)
                    .forEach(m -> sb.append("- ").append(m).append("\n"));
            sb.append("\n");
        }

        // 召回的世界观片段
        if (!ctx.retrievedContexts.isEmpty()) {
            sb.append("【背景参考资料（择要使用）】\n");
            ctx.retrievedContexts.stream()
                    .filter(rc -> !rc.source().equals("memory_l0"))
                    .limit(4)
                    .forEach(rc -> sb.append("- [").append(rc.source()).append("] ")
                            .append(shorten(rc.text(), 100)).append("\n"));
            sb.append("\n");
        }

        // 玩家行动
        sb.append("【玩家行动】\n").append(ctx.playerInput).append("\n\n");
        sb.append("【意图类型】").append(ctx.intent).append("\n\n");
        sb.append("请生成本回合叙事：");

        return sb.toString();
    }

    // ===== 模板降级 =====

    private String templateFallback(String playerInput, String location, String intent) {
        String env = switch (location) {
            case "safe_house"      -> "破旧的避难所内弥漫着汽油与铁锈的气味，窗缝透进稀薄的灰色天光";
            case "old_gas_station" -> "废弃加油站的油漆早已剥落，停滞的空气里混着腐败物与机油的腥气";
            case "subway_ruins"    -> "地铁废墟深处，远处偶尔传来金属滚动的回响，黑暗沉甸甸地压着你";
            default                -> "末日的废土上，时间似乎已经失去了意义";
        };
        String action = switch (intent != null ? intent : "") {
            case "COMBAT"       -> "你握紧了手中的武器，肌肉在绷紧的一瞬间感到一阵酸痛";
            case "FREE_EXPLORE" -> "你放轻脚步，贴着墙壁向前摸索，尽量不发出任何声响";
            case "RULE_QUERY"   -> "你在脑海中快速整理着已知的规则与技能，试图找到最优解";
            default             -> "你压低呼吸，按照心中的计划一步步推进——" + shorten(playerInput, 30);
        };
        return env + "。" + action + "。"
                + "体内残存的体力像沙漏里最后几粒沙，在每一次呼吸间悄悄流失。"
                + "远处有细微的响动，像是某种东西正在逼近，但你无暇顾及，"
                + "眼前的局面已经容不下任何犹豫——你必须立刻做出选择。";
    }

    private String shorten(String text, int max) {
        if (text == null || text.length() <= max) return text;
        return text.substring(0, max - 1) + "…";
    }
}

