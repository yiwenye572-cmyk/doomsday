package com.doomsday.game.tool;

import java.util.List;
import java.util.Map;

public interface ToolDefinition {

    String name();

    String description();

    default boolean sideEffect() {
        return false;
    }

    default List<String> requiredPayloadFields() {
        return List.of();
    }

    default List<String> validate(Map<String, Object> payload) {
        if (payload == null) {
            return requiredPayloadFields().isEmpty()
                    ? List.of()
                    : List.of("payload is required");
        }
        return requiredPayloadFields().stream()
                .filter(field -> !payload.containsKey(field))
                .map(field -> "missing field: " + field)
                .toList();
    }

    Map<String, Object> execute(ToolContext context, Map<String, Object> payload);
}
