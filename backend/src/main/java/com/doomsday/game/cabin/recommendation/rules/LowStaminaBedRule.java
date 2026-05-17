package com.doomsday.game.cabin.recommendation.rules;

import com.doomsday.game.cabin.recommendation.LayoutRule;
import com.doomsday.game.cabin.recommendation.RecommendedItem;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 规则 R01 — 低体力时将床置于画布中央可达位置。
 *
 * 触发条件：playerStamina <= 40 且列表中存在 type=bed 的物品。
 * 效果：床移到 (192, 160)（600×400 中央左区）。
 */
@Component
public class LowStaminaBedRule implements LayoutRule {

    // 目标位置：画布中央偏左（方便玩家快速点击）
    private static final int BED_X = 192;
    private static final int BED_Y = 160;
    private static final int STAMINA_THRESHOLD = 40;

    @Override
    public String name() { return "低体力→床置中央"; }

    @Override
    public int priority() { return 10; }

    @Override
    public boolean apply(List<RecommendedItem> items, int stamina,
                         String timeOfDay, Map<String, Object> stateData) {
        if (stamina > STAMINA_THRESHOLD) return false;
        boolean triggered = false;
        for (int i = 0; i < items.size(); i++) {
            if ("bed".equalsIgnoreCase(items.get(i).type())) {
                items.set(i, items.get(i).withPosition(BED_X, BED_Y));
                triggered = true;
            }
        }
        return triggered;
    }
}
