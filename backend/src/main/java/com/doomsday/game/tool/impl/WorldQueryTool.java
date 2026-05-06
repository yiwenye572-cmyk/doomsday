package com.doomsday.game.tool.impl;

import com.doomsday.game.domain.GameSession;
import com.doomsday.game.tool.ToolContext;
import com.doomsday.game.tool.ToolDefinition;
import com.doomsday.game.world.EventCardRepository;
import com.doomsday.game.world.LorebookEntryRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class WorldQueryTool implements ToolDefinition {

    private final EventCardRepository eventCardRepository;
    private final LorebookEntryRepository lorebookEntryRepository;

    public WorldQueryTool(EventCardRepository eventCardRepository,
                          LorebookEntryRepository lorebookEntryRepository) {
        this.eventCardRepository = eventCardRepository;
        this.lorebookEntryRepository = lorebookEntryRepository;
    }

    @Override
    public String name() {
        return "WorldQueryTool";
    }

    @Override
    public String description() {
        return "查询当前世界版本下的事件卡与 lore 片段";
    }

    @Override
    public List<String> requiredPayloadFields() {
        return List.of("query");
    }

    @Override
    public Map<String, Object> execute(ToolContext context, Map<String, Object> payload) {
        String query = String.valueOf(payload.getOrDefault("query", "")).trim();
        int topK = toInt(payload.get("topK"), 3);

        GameSession session = context == null ? null : context.session();
        String worldVersion = session == null ? "world_v1" : session.getWorldVersion();
        String location = session == null ? "safe_house" : session.getLocation();

        List<Map<String, Object>> hits = new ArrayList<>();
        eventCardRepository.findTopByLocationTagAndVersion(location, worldVersion, topK)
                .forEach(card -> hits.add(Map.of(
                        "source", "event_card",
                        "id", card.getEventId(),
                        "snippet", shorten(card.getTriggerJson(), 120)
                )));
        lorebookEntryRepository.findTopByKeywordAndVersion(query.isBlank() ? location : query, worldVersion, topK)
                .forEach(entry -> hits.add(Map.of(
                        "source", "lorebook",
                        "id", entry.getEntryId(),
                        "snippet", shorten(entry.getBody(), 120)
                )));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("worldVersion", worldVersion);
        result.put("query", query);
        result.put("hits", hits);
        return result;
    }

    private int toInt(Object value, int defaultValue) {
        if (!(value instanceof Number number)) {
            return defaultValue;
        }
        int resolved = number.intValue();
        return Math.max(1, Math.min(resolved, 8));
    }

    private String shorten(String text, int max) {
        if (text == null || text.length() <= max) {
            return text == null ? "" : text;
        }
        return text.substring(0, max - 1) + "...";
    }
}
