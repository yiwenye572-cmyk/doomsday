package com.doomsday.game.cabin.recommendation;

/**
 * 推荐布局中的单个物品快照。
 *
 * 位置坐标以 32px 网格为基准（与 CabinCanvas 保持一致）。
 */
public record RecommendedItem(
        String id,
        String type,
        int x,
        int y,
        int w,
        int h,
        int rotation   // 0 | 90 | 180 | 270
) {
    /** 快捷工厂：宽/高默认 64x64，不旋转 */
    public static RecommendedItem of(String id, String type, int x, int y) {
        return new RecommendedItem(id, type, x, y, 64, 64, 0);
    }

    /** 返回副本并修改坐标 */
    public RecommendedItem withPosition(int nx, int ny) {
        return new RecommendedItem(id, type, nx, ny, w, h, rotation);
    }
}
