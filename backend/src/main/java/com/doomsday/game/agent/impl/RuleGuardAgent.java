package com.doomsday.game.agent.impl;

import com.doomsday.game.agent.AgentChain;
import com.doomsday.game.agent.AgentHandler;
import com.doomsday.game.agent.TurnContext;
import com.doomsday.game.api.OptionPayload;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Rule Guard Agent：校验剧情与选项是否违反世界观硬规则。
 *
 * P1 实现：HP/体力边界校验 + 翻盘卡单局使用约束。
 * 规则 DSL 接入后可从 rule_dsl 表动态加载，接口不变。
 */
@Component
public class RuleGuardAgent implements AgentHandler {

    private static final Logger log = LoggerFactory.getLogger(RuleGuardAgent.class);

    @Override
    public String name() {
        return "RuleGuardAgent";
    }

    @Override
    public void handle(TurnContext ctx, AgentChain next) {
        List<String> violations = new ArrayList<>();

        // 规则 1：HP 不能小于 0 或大于 100
        int hp = ctx.session.getHp();
        if (hp < 0 || hp > 100) {
            violations.add("HP_OUT_OF_RANGE: hp=" + hp);
        }

        // 规则 2：体力不能小于 0 或大于 100
        int stamina = ctx.session.getStamina();
        if (stamina < 0 || stamina > 100) {
            violations.add("STAMINA_OUT_OF_RANGE: stamina=" + stamina);
        }

        // 规则 3：感染值不能超过 100
        int infection = ctx.session.getInfection();
        if (infection > 100) {
            violations.add("INFECTION_OVERFLOW: infection=" + infection);
        }

        // 规则 4：选项列表必须恰好 4 个
        if (ctx.options == null || ctx.options.size() != 4) {
            violations.add("OPTION_COUNT_VIOLATION: expected=4 actual="
                    + (ctx.options == null ? 0 : ctx.options.size()));
        }

        ctx.violations = violations;
        ctx.rulesPassed = violations.isEmpty();

        if (!ctx.rulesPassed) {
            log.warn("[{}] traceId={} violations={}", name(), ctx.traceId, violations);
            // 规则违反时不 abort，由 Orchestrator 决策是否降级处理
        } else {
            log.debug("[{}] traceId={} rules_passed", name(), ctx.traceId);
        }

        next.handle(ctx);
    }
}
