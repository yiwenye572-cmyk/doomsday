package com.doomsday.game.diary;

import com.doomsday.game.diary.dto.DiaryEntryView;
import com.doomsday.game.diary.dto.DiaryLevel;
import com.doomsday.game.diary.model.DiaryEntryL1;
import com.doomsday.game.diary.model.DiaryEntryL2;
import com.doomsday.game.diary.repo.GameDiaryRepository;
import com.doomsday.game.domain.GameSession;
import com.doomsday.game.domain.SessionRepository;
import com.doomsday.game.domain.TurnMemory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GameDiaryService {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final SessionRepository sessionRepository;
    private final GameDiaryRepository diaryRepository;
    private final ObjectMapper objectMapper;
    private final int summaryEveryTurns;

    public GameDiaryService(SessionRepository sessionRepository,
                            GameDiaryRepository diaryRepository,
                            ObjectMapper objectMapper,
                            @Value("${game.diary.summary.every-turns:3}") int summaryEveryTurns) {
        this.sessionRepository = sessionRepository;
        this.diaryRepository = diaryRepository;
        this.objectMapper = objectMapper;
        this.summaryEveryTurns = Math.max(1, summaryEveryTurns);
    }

    public List<DiaryEntryView> queryDiary(String sessionId, DiaryLevel level, Integer fromTurn, Integer toTurn) {
        return switch (level) {
            case L0 -> queryL0(sessionId, fromTurn, toTurn);
            case L1 -> queryL1(sessionId, fromTurn, toTurn);
            case L2 -> queryL2(sessionId, fromTurn, toTurn);
        };
    }

    public SummaryResult summarizeRange(String sessionId, Integer fromTurn, Integer toTurn, String source) {
        List<TurnMemory> memories = sessionRepository.findRecentTurnMemories(sessionId, 120).stream()
                .filter(m -> fromTurn == null || m.turn() >= fromTurn)
                .filter(m -> toTurn == null || m.turn() <= toTurn)
                .toList();
        if (memories.isEmpty()) {
            return new SummaryResult(sessionId, false, 0, 0, "L1", "");
        }

        int resolvedFrom = memories.get(0).turn();
        int resolvedTo = memories.get(memories.size() - 1).turn();
        String summary = buildSummary(memories);

        GameSession session = sessionRepository.findById(sessionId);
        DiaryEntryL1 entry = new DiaryEntryL1();
        entry.setSessionId(sessionId);
        entry.setWorldVersion(session == null ? null : session.getWorldVersion());
        entry.setFromTurn(resolvedFrom);
        entry.setToTurn(resolvedTo);
        entry.setSummary(summary);
        entry.setTagsJson(toJson(extractTopTags(memories, 8)));
        entry.setSource(source == null || source.isBlank() ? "MANUAL" : source);
        diaryRepository.saveL1(entry);

        maybeGenerateL2(sessionId, session == null ? null : session.getWorldVersion());

        return new SummaryResult(sessionId, true, resolvedFrom, resolvedTo, "L1", summary);
    }

    public SummaryResult maybeSummarizeByTurn(String sessionId) {
        List<TurnMemory> memories = sessionRepository.findRecentTurnMemories(sessionId, 120);
        if (memories.isEmpty()) {
            return new SummaryResult(sessionId, false, 0, 0, "L1", "");
        }

        int lastTurn = memories.get(memories.size() - 1).turn();
        int lastSummarized = diaryRepository.findL1LastTurn(sessionId);
        if ((lastTurn - lastSummarized) < summaryEveryTurns) {
            return new SummaryResult(sessionId, false, 0, 0, "L1", "");
        }

        int fromTurn = Math.max(1, lastSummarized + 1);
        return summarizeRange(sessionId, fromTurn, lastTurn, "AUTO");
    }

    private List<DiaryEntryView> queryL0(String sessionId, Integer fromTurn, Integer toTurn) {
        return sessionRepository.findRecentTurnMemories(sessionId, 120).stream()
                .filter(m -> fromTurn == null || m.turn() >= fromTurn)
                .filter(m -> toTurn == null || m.turn() <= toTurn)
                .map(m -> new DiaryEntryView(
                        "L0",
                        m.turn(),
                        m.turn(),
                        compactL0(m),
                        sanitizeList(m.rewardFlags()),
                        m.timestamp()))
                .toList();
    }

    private List<DiaryEntryView> queryL1(String sessionId, Integer fromTurn, Integer toTurn) {
        return diaryRepository.findL1(sessionId, fromTurn, toTurn, 80).stream()
                .map(e -> new DiaryEntryView(
                        "L1",
                        e.getFromTurn(),
                        e.getToTurn(),
                        e.getSummary(),
                        parseTags(e.getTagsJson()),
                        toEpoch(e.getCreatedAt())))
                .toList();
    }

    private List<DiaryEntryView> queryL2(String sessionId, Integer fromTurn, Integer toTurn) {
        return diaryRepository.findL2(sessionId, fromTurn, toTurn, 80).stream()
                .map(e -> new DiaryEntryView(
                        "L2",
                        e.getFromTurn(),
                        e.getToTurn(),
                        e.getTopic() + "：" + e.getSummary(),
                        parseTags(e.getKeyFactsJson()),
                        toEpoch(e.getCreatedAt())))
                .toList();
    }

    private void maybeGenerateL2(String sessionId, String worldVersion) {
        List<DiaryEntryL1> latest = diaryRepository.findL1(sessionId, null, null, 5);
        if (latest.size() < 5) {
            return;
        }

        int minTurn = latest.stream().mapToInt(DiaryEntryL1::getFromTurn).min().orElse(1);
        int maxTurn = latest.stream().mapToInt(DiaryEntryL1::getToTurn).max().orElse(minTurn);
        String merged = latest.stream()
                .map(DiaryEntryL1::getSummary)
                .collect(Collectors.joining(" "));

        DiaryEntryL2 l2 = new DiaryEntryL2();
        l2.setSessionId(sessionId);
        l2.setWorldVersion(worldVersion);
        l2.setFromTurn(minTurn);
        l2.setToTurn(maxTurn);
        l2.setTopic("阶段剧情");
        l2.setSummary(shorten(merged, 220));
        l2.setKeyFactsJson(toJson(extractTopTagsFromText(merged, 10)));
        l2.setSource("AUTO");
        diaryRepository.saveL2(l2);
    }

    private String buildSummary(List<TurnMemory> memories) {
        int from = memories.get(0).turn();
        int to = memories.get(memories.size() - 1).turn();
        int totalLoss = memories.stream().mapToInt(m -> Math.max(0, m.staminaLoss())).sum();

        String intents = memories.stream()
                .map(TurnMemory::intent)
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()))
                .entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.joining("/"));

        String highlights = memories.stream()
                .map(TurnMemory::narration)
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .map(s -> shorten(s, 26))
                .limit(3)
                .collect(Collectors.joining("；"));

        if (intents.isBlank()) {
            intents = "FREE_EXPLORE";
        }

        return "回合" + from + "-" + to
                + "，主意图=" + intents
                + "，体力总损耗=" + totalLoss
                + "，事件摘要=" + highlights;
    }

    private String compactL0(TurnMemory memory) {
        return "输入=" + fallback(memory.playerInput())
                + "；意图=" + fallback(memory.intent())
                + "；体力损耗=" + Math.max(0, memory.staminaLoss())
                + "；摘要=" + shorten(memory.narration(), 40);
    }

    private List<String> extractTopTags(List<TurnMemory> memories, int maxSize) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        for (TurnMemory memory : memories) {
            sanitizeList(memory.rewardFlags()).forEach(merged::add);
            if (memory.intent() != null && !memory.intent().isBlank()) {
                merged.add(memory.intent());
            }
        }
        if (merged.isEmpty()) {
            merged.add("NO_REWARD");
        }
        return merged.stream().limit(maxSize).toList();
    }

    private List<String> extractTopTagsFromText(String text, int maxSize) {
        if (text == null || text.isBlank()) {
            return List.of("SUMMARY");
        }
        String[] parts = text.split("[^A-Za-z0-9_\\u4e00-\\u9fa5]+");
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String part : parts) {
            if (part == null || part.isBlank() || part.length() < 2) {
                continue;
            }
            set.add(part);
            if (set.size() >= maxSize) {
                break;
            }
        }
        if (set.isEmpty()) {
            set.add("SUMMARY");
        }
        return new ArrayList<>(set);
    }

    private List<String> parseTags(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<String> parsed = objectMapper.readValue(json, STRING_LIST);
            return sanitizeList(parsed);
        } catch (Exception ignore) {
            return List.of();
        }
    }

    private List<String> sanitizeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .limit(12)
                .toList();
    }

    private String toJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (Exception e) {
            return "[]";
        }
    }

    private long toEpoch(OffsetDateTime time) {
        if (time == null) {
            return Instant.now().toEpochMilli();
        }
        return time.toInstant().toEpochMilli();
    }

    private String shorten(String text, int max) {
        String value = fallback(text);
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max - 1) + "...";
    }

    private String fallback(String text) {
        return (text == null || text.isBlank()) ? "无" : text;
    }

    public record SummaryResult(
            String sessionId,
            boolean created,
            int fromTurn,
            int toTurn,
            String level,
            String summary
    ) {
    }
}
