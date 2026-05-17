package com.doomsday.game.cabin.recommendation.rules;

import com.doomsday.game.cabin.recommendation.LayoutRule;
import com.doomsday.game.cabin.recommendation.RecommendedItem;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 规则 R04 — 夜间/傍晚时窗户置于画布上方（采光位）。
 *
 * 触发条件：timeOfDay 为 night 或 evening，且存在 type=window。
 * 效果：窗户移到 (320, 32)（画布顶部中央）。
 */
@Component
public class WindowLightRule implements LayoutRule {

    private static final int WINDOW_X = 320;
    private static final int WINDOW_Y = 32;

    @Override
    public String name() { return "夜间窗户置顶采光"; }

    @Override
    public int priority() { return 15; }

    @Override
    public boolean apply(List<RecommendedItem> items, int stamina,
                         String timeOfDay, Map<String, Object> stateData) {
        boolean isNight = "night".equalsIgnoreCase(timeOfDay)
                || "evening".equalsIgnoreCase(timeOfDay);
        if (!isNight) return false;

        boolean triggered = false;
        for (int i = 0; i < items.size(); i++) {
            if ("window".equalsIgnoreCase(items.get(i).type())) {
                items.set(i, items.get(i).withPosition(WINDOW_X, WINDOW_Y));
                triggered = true;
            }
        }
        return triggered;
    }
}
