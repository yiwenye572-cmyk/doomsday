package com.doomsday.game.admin;

import java.util.Map;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class CacheMetricsStore {

    private static final String PREFIX = "game:metrics:cache:";

    private final StringRedisTemplate redis;

    public CacheMetricsStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void recordHit(String scope) {
        increment(scope, "hit");
    }

    public void recordMiss(String scope) {
        increment(scope, "miss");
    }

    public CacheSnapshot getSnapshot() {
        long hit = 0;
        long miss = 0;
        Set<String> keys = redis.keys(PREFIX + "*");
        if (keys != null) {
            for (String key : keys) {
                Map<Object, Object> raw = redis.opsForHash().entries(key);
                hit += parseLong(raw, "hit");
                miss += parseLong(raw, "miss");
            }
        }
        long total = hit + miss;
        double hitRate = total > 0 ? (double) hit / total : 0.0;
        return new CacheSnapshot(hit, miss, hitRate);
    }

    private void increment(String scope, String field) {
        try {
            redis.opsForHash().increment(PREFIX + scope, field, 1);
        } catch (Exception ignored) {
            // 指标失败不能影响主链路
        }
    }

    private long parseLong(Map<Object, Object> map, String field) {
        Object v = map.get(field);
        if (v == null) return 0;
        try {
            return Long.parseLong(v.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public record CacheSnapshot(long hit, long miss, double hitRate) {}
}