package com.doomsday.game.tool;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class ToolRegistry {

    private final Map<String, ToolDefinition> tools = new ConcurrentHashMap<>();

    public ToolRegistry(List<ToolDefinition> definitions) {
        definitions.forEach(tool -> tools.put(tool.name(), tool));
    }

    public Optional<ToolDefinition> find(String toolName) {
        return Optional.ofNullable(tools.get(toolName));
    }

    public List<ToolDefinition> all() {
        return tools.values().stream()
                .sorted(Comparator.comparing(ToolDefinition::name))
                .toList();
    }
}
