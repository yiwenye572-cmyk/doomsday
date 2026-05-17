package com.doomsday.game.cabin.recommendation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 布局推荐引擎（规则驱动）
 *
 * 流程：
 *   1. 从 stateData 解析现有物品列表（items）
 *   2. 生成初始推荐条目（保持原物品类型，坐标先走默认网格排列）
 *   3. 按 priority 顺序依次执行所有 {@link LayoutRule}
 *   4. 收集触发的规则名称 → reason 字段
 *   5. 计算置信度（= 触发规则数 / 全部规则数 * 0.8 + 0.2 基线）
 *
 * 可扩展：增加 @Component 实现 LayoutRule 接口即可自动注册。
 */
@Component
public class LayoutRecommendationEngine {

    private static final Logger log = LoggerFactory.getLogger(LayoutRecommendationEngine.class);

    /** 默认初始网格起点（32px 步长） */
    private static final int GRID  = 32;
    private static final int COLS  = 8;   // 每行最多摆 8 件
    private static final int START_X = 64;
    private static final int START_Y = 64;

    private final List<LayoutRule> rules;
    private final ObjectMapper objectMapper;

    public LayoutRecommendationEngine(List<LayoutRule> rules, ObjectMapper objectMapper) {
        // 按 priority 升序排列（越小越优先）
        this.rules = rules.stream()
                .sorted(Comparator.comparingInt(LayoutRule::priority))
                .toList();
        this.objectMapper = objectMapper;
        log.info("[RecommendationEngine] loaded {} rules: {}", rules.size(),
                this.rules.stream().map(LayoutRule::name).toList());
    }

    /**
     * 执行推荐。
     *
     * @param stateData   当前 stateData JSON 字符串
     * @param stamina     玩家体力
     * @param timeOfDay   当前时间段
     * @return 推荐结果（含 items、reason、confidence）
     */
    public RecommendationResult recommend(String stateData, int stamina, String timeOfDay) {
        Map<String, Object> stateMap = parseState(stateData);

        // 1. 提取或构建初始物品列表
        List<RecommendedItem> items = buildInitialItems(stateMap);

        // 2. 逐规则应用
        List<String> triggeredReasons = new ArrayList<>();
        for (LayoutRule rule : rules) {
            try {
                if (rule.apply(items, stamina, timeOfDay, stateMap)) {
                    triggeredReasons.add(rule.name());
                }
            } catch (Exception e) {
                log.warn("[RecommendationEngine] rule '{}' threw exception: {}", rule.name(), e.getMessage());
            }
        }

        // 3. 计算置信度
        double confidence = rules.isEmpty() ? 0.5
                : 0.20 + 0.80 * ((double) triggeredReasons.size() / rules.size());
        confidence = Math.min(1.0, confidence);

        String reason = triggeredReasons.isEmpty()
                ? "默认网格布局（无特殊规则触发）"
                : String.join("，", triggeredReasons);

        return new RecommendationResult(items, reason, confidence);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseState(String stateData) {
        if (stateData == null || stateData.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(stateData, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }

    /**
     * 从 stateMap["items"] 提取现有物品；若无则返回默认示例集合。
     */
    @SuppressWarnings("unchecked")
    private List<RecommendedItem> buildInitialItems(Map<String, Object> stateMap) {
        Object rawItems = stateMap.get("items");
        List<Map<String, Object>> itemList = List.of();
        if (rawItems instanceof List<?> list) {
            itemList = (List<Map<String, Object>>) list;
        }

        if (itemList.isEmpty()) {
            // 无已有物品：返回示范布局供前端预览
            return defaultDemoItems();
        }

        List<RecommendedItem> result = new ArrayList<>();
        int col = 0;
        for (Map<String, Object> it : itemList) {
            String id   = toString(it.get("id"),   "item_" + result.size());
            String type = toString(it.get("type"), "unknown");
            int w = toInt(it.get("w"), 64);
            int h = toInt(it.get("h"), 64);
            int rotation = toInt(it.get("rotation"), 0);
            // 初始位置：按列排布（规则会覆盖）
            int x = START_X + (col % COLS) * (GRID * 3);
            int y = START_Y + (col / COLS) * (GRID * 3);
            col++;
            result.add(new RecommendedItem(id, type, x, y, w, h, rotation));
        }
        return result;
    }

    /** 没有任何物品时的示例布局（让 Demo 也能看到效果） */
    private List<RecommendedItem> defaultDemoItems() {
        return new ArrayList<>(List.of(
                new RecommendedItem("bed_demo",    "bed",      192, 160, 128, 64, 0),
                new RecommendedItem("window_demo", "window",   320,  32,  64, 64, 0),
                new RecommendedItem("table_demo",  "table",     64,  64,  64, 64, 0),
                new RecommendedItem("medkit_demo", "medkit",   320, 160,  64, 64, 0),
                new RecommendedItem("axe_demo",    "axe",      480, 320,  64, 64, 0)
        ));
    }

    private String toString(Object v, String def) {
        return v != null ? v.toString() : def;
    }

    private int toInt(Object v, int def) {
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {}
        }
        return def;
    }

    // ─── Result value object ─────────────────────────────────────────────

    public record RecommendationResult(
            List<RecommendedItem> items,
            String reason,
            double confidence
    ) {}
}
