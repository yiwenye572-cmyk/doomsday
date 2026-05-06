package com.doomsday.game.tool.impl;

import com.doomsday.game.domain.GameSession;
import com.doomsday.game.domain.SessionRepository;
import com.doomsday.game.tool.ToolContext;
import com.doomsday.game.tool.ToolDefinition;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class EntityStatePatchTool implements ToolDefinition {

    private final SessionRepository sessionRepository;

    public EntityStatePatchTool(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Override
    public String name() {
        return "EntityStatePatchTool";
    }

    @Override
    public String description() {
        return "对会话状态执行标准化 Patch（支持 dryRun）";
    }

    @Override
    public boolean sideEffect() {
        return true;
    }

    @Override
    public Map<String, Object> execute(ToolContext context, Map<String, Object> payload) {
        GameSession session = context == null ? null : context.session();
        if (session == null) {
            throw new IllegalArgumentException("session is required for EntityStatePatchTool");
        }

        boolean dryRun = toBoolean(payload.get("dryRun"), true);

        int hpDelta = toInt(payload.get("hpDelta"));
        int staminaDelta = toInt(payload.get("staminaDelta"));
        int infectionDelta = toInt(payload.get("infectionDelta"));
        String location = payload.get("location") == null ? null : String.valueOf(payload.get("location")).trim();
        List<String> inventoryAdd = toStringList(payload.get("inventoryAdd"));

        Map<String, Object> before = snapshot(session);

        session.setHp(clamp(session.getHp() + hpDelta, 0, 100));
        session.setStamina(clamp(session.getStamina() + staminaDelta, 0, 100));
        session.setInfection(clamp(session.getInfection() + infectionDelta, 0, 100));
        if (location != null && !location.isBlank()) {
            session.setLocation(location);
        }
        if (!inventoryAdd.isEmpty()) {
            List<String> merged = new ArrayList<>(session.getInventory());
            merged.addAll(inventoryAdd);
            session.setInventory(merged.stream().distinct().toList());
        }

        if (toBoolean(payload.get("forceFailAfterApply"), false)) {
            throw new IllegalStateException("forced failure after apply");
        }

        if (!dryRun) {
            session.setVersion(session.getVersion() + 1);
            sessionRepository.save(session);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dryRun", dryRun);
        result.put("before", before);
        result.put("after", snapshot(session));
        return result;
    }

    private Map<String, Object> snapshot(GameSession session) {
        return Map.of(
                "version", session.getVersion(),
                "hp", session.getHp(),
                "stamina", session.getStamina(),
                "infection", session.getInfection(),
                "location", session.getLocation(),
                "inventory", session.getInventory()
        );
    }

    private int toInt(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private boolean toBoolean(Object value, boolean defaultValue) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return defaultValue;
    }

    private List<String> toStringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(item -> item != null)
                .map(String::valueOf)
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
