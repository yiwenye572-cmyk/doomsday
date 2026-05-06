package com.doomsday.game.tool.impl;

import com.doomsday.game.domain.SessionRepository;
import com.doomsday.game.domain.TurnMemory;
import com.doomsday.game.tool.ToolContext;
import com.doomsday.game.tool.ToolDefinition;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class MemoryRecallTool implements ToolDefinition {

    private final SessionRepository sessionRepository;

    public MemoryRecallTool(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Override
    public String name() {
        return "MemoryRecallTool";
    }

    @Override
    public String description() {
        return "回溯 L0/L1 记忆片段供当前推理引用";
    }

    @Override
    public Map<String, Object> execute(ToolContext context, Map<String, Object> payload) {
        String sessionId = context == null ? null : context.sessionId();
        if ((sessionId == null || sessionId.isBlank()) && payload.get("sessionId") != null) {
            sessionId = String.valueOf(payload.get("sessionId")).trim();
        }
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId is required for memory recall");
        }

        int limit = toInt(payload.get("limit"), 4);
        boolean includeEpisodic = toBoolean(payload.get("includeEpisodic"), true);

        List<Map<String, Object>> l0 = sessionRepository.findRecentTurnMemories(sessionId, limit).stream()
                .map(this::toMap)
                .toList();
        List<String> l1 = includeEpisodic
                ? sessionRepository.findRecentEpisodicSummaries(sessionId, Math.min(3, limit))
                : List.of();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", sessionId);
        result.put("l0", l0);
        result.put("l1", l1);
        return result;
    }

    private Map<String, Object> toMap(TurnMemory m) {
        return Map.of(
                "turn", m.turn(),
                "intent", fallback(m.intent()),
                "summary", shorten(m.narration(), 90)
        );
    }

    private String fallback(String value) {
        return value == null || value.isBlank() ? "FREE_EXPLORE" : value;
    }

    private String shorten(String text, int max) {
        if (text == null || text.length() <= max) {
            return text == null ? "" : text;
        }
        return text.substring(0, max - 1) + "...";
    }

    private int toInt(Object value, int defaultValue) {
        if (!(value instanceof Number number)) {
            return defaultValue;
        }
        return Math.max(1, Math.min(number.intValue(), 10));
    }

    private boolean toBoolean(Object value, boolean defaultValue) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return defaultValue;
    }
}
