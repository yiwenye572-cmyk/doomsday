package com.doomsday.game.cabin.recommendation;

import java.util.List;
import java.util.Map;

/**
 * 单条布局推荐规则接口。
 *
 * 每个 Rule 按优先级（priority）排列，
 * {@link LayoutRecommendationEngine} 按序调用，规则可以
 * 相互叠加（不互斥），通过修改 {@code items} 列表实现。
 */
public interface LayoutRule {

    /** 规则人类可读名称（用于 reason 字段）。 */
    String name();

    /** 数值越小越先执行。 */
    int priority();

    /**
     * 对推荐布局列表执行原地调整。
     *
     * @param items       当前推荐条目（可修改位置/顺序）
     * @param stamina     玩家体力值（0-100）
     * @param timeOfDay   当前时间段（morning/afternoon/evening/night）
     * @param stateData   完整 stateData Map（只读，供规则读取自定义字段）
     * @return 触发了该规则时返回 true（用于生成 reason 列表）
     */
    boolean apply(List<RecommendedItem> items, int stamina, String timeOfDay,
                  Map<String, Object> stateData);
}
