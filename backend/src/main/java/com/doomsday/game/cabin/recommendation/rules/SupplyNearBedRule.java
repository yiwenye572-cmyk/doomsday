package com.doomsday.game.cabin.recommendation.rules;

import com.doomsday.game.cabin.recommendation.LayoutRule;
import com.doomsday.game.cabin.recommendation.RecommendedItem;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 规则 R03 — 消耗品（食物/医疗）摆放在床附近（右侧 96px 区域）。
 *
 * 触发条件：列表中同时存在 bed 和 supply/food/medicine/medkit。
 * 效果：消耗品 x = bed.x + 96，y = bed.y；若无床则放在固定位置 (320, 160)。
 */
@Component
public class SupplyNearBedRule implements LayoutRule {

    private static final Set<String> SUPPLY_TYPES = Set.of(
            "supply", "food", "medicine", "medkit", "water", "bandage", "pill"
    );

    @Override
    public String name() { return "消耗品贴近床铺"; }

    @Override
    public int priority() { return 30; }

    @Override
    public boolean apply(List<RecommendedItem> items, int stamina,
                         String timeOfDay, Map<String, Object> stateData) {
        // 找床的位置
        int bedX = 320, bedY = 160;
        for (RecommendedItem item : items) {
            if ("bed".equalsIgnoreCase(item.type())) {
                bedX = item.x();
                bedY = item.y();
                break;
            }
        }

        int slot = 0;
        boolean triggered = false;
        for (int i = 0; i < items.size(); i++) {
            String type = items.get(i).type() != null
                    ? items.get(i).type().toLowerCase() : "";
            if (SUPPLY_TYPES.contains(type)) {
                // 消耗品排成一列，紧靠床右侧
                items.set(i, items.get(i).withPosition(bedX + 96 + slot * 80, bedY));
                slot++;
                triggered = true;
            }
        }
        return triggered;
    }
}
