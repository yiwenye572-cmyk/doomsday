package com.doomsday.game.cabin.recommendation.rules;

import com.doomsday.game.cabin.recommendation.LayoutRule;
import com.doomsday.game.cabin.recommendation.RecommendedItem;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 规则 R02 — 武器/工具靠近出口（右下角区域）摆放。
 *
 * 触发条件：列表中存在 type 属于 weapon/tool/axe/knife/gun 等的物品。
 * 效果：将武器/工具移到右下区域（起始 x=480, y=320），间距 80px。
 */
@Component
public class WeaponNearExitRule implements LayoutRule {

    private static final Set<String> WEAPON_TYPES = Set.of(
            "weapon", "tool", "axe", "knife", "gun", "spear", "bow", "crossbow"
    );

    // 出口区域（画布右下）
    private static final int EXIT_X = 480;
    private static final int EXIT_Y = 320;
    private static final int SPACING = 80;

    @Override
    public String name() { return "武器/工具靠近出口"; }

    @Override
    public int priority() { return 20; }

    @Override
    public boolean apply(List<RecommendedItem> items, int stamina,
                         String timeOfDay, Map<String, Object> stateData) {
        int slot = 0;
        boolean triggered = false;
        for (int i = 0; i < items.size(); i++) {
            String type = items.get(i).type() != null
                    ? items.get(i).type().toLowerCase() : "";
            if (WEAPON_TYPES.contains(type)) {
                items.set(i, items.get(i).withPosition(EXIT_X + slot * SPACING, EXIT_Y));
                slot++;
                triggered = true;
            }
        }
        return triggered;
    }
}
