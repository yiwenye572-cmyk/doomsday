package com.doomsday.game.agent.impl;

import com.doomsday.game.agent.AgentChain;
import com.doomsday.game.agent.AgentHandler;
import com.doomsday.game.agent.TurnContext;
import com.doomsday.game.agent.TurnContext.RetrievedContext;
import com.doomsday.game.common.RetryExecutor;
import com.doomsday.game.world.EventCardRepository;
import com.doomsday.game.world.LorebookEntry;
import com.doomsday.game.world.LorebookEntryRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

/**
 * Retrieval Agent：多路召回，为 Plot 生成提供世界观证据。
 *
 * 召回顺序（优先级从高到低）：
 *   1. VectorStore 语义相似度（Spring AI PgVectorStore + DashScope text-embedding-v3）
 *   2. JPA 事件卡（按当前位置标签精确匹配）
 *   3. Lorebook 混合召回（关键词 + 向量）
 *   4. 去重与多样性重排（来源配额 + 文本去重）
 *   5. L0/L1 Memory（由 TurnOrchestrator 在链路开始时注入，此处仅参与重排）
 */
@Component
public class RetrievalAgent implements AgentHandler {

    private static final Logger log = LoggerFactory.getLogger(RetrievalAgent.class);
    private static final int TOP_K = 3;
    private static final int FINAL_TOP_K = 8;
    private static final double SIMILARITY_THRESHOLD = 0.55;
    private static final double W_VECTOR = 0.65;
    private static final double W_KEYWORD = 0.35;

    private final VectorStore vectorStore;
    private final EventCardRepository eventCardRepo;
    private final LorebookEntryRepository lorebookRepo;

    public RetrievalAgent(VectorStore vectorStore,
                          EventCardRepository eventCardRepo,
                          LorebookEntryRepository lorebookRepo) {
        this.vectorStore = vectorStore;
        this.eventCardRepo = eventCardRepo;
        this.lorebookRepo = lorebookRepo;
    }

    @Override
    public String name() {
        return "RetrievalAgent";
    }

    @Override
    public void handle(TurnContext ctx, AgentChain next) {
        List<Document> docs = List.of();

        // --- 1. 语义向量检索（Spring AI VectorStore）---
        try {
            docs = RetryExecutor.run(2, () -> vectorStore.similaritySearch(
                SearchRequest.builder()
                    .query(ctx.playerInput)
                    .topK(TOP_K * 2)
                    .similarityThreshold(SIMILARITY_THRESHOLD)
                    .build()
            ));
            docs.forEach(doc -> {
                String source = (String) doc.getMetadata().getOrDefault("source", "vector");
                double score = extractVectorScore(doc);
                ctx.retrievedContexts.add(new RetrievedContext(source, doc.getId(), doc.getText(), score));
            });
        } catch (Exception e) {
            log.warn("[{}] traceId={} vector search failed: {}", name(), ctx.traceId, e.getMessage());
        }

        // --- 2. JPA 事件卡（位置标签精确匹配）---
        String location = ctx.session.getLocation();
        String worldVersion = ctx.session.getWorldVersion();
        eventCardRepo.findTopByLocationTagAndVersion(location, worldVersion, TOP_K)
                .forEach(ec -> ctx.retrievedContexts.add(
                        new RetrievedContext("event_card", ec.getEventId(), ec.getTriggerJson(), 0.85)
                ));

        // --- 3. Lorebook 混合召回（关键词 + 向量）并做评分融合 ---
        List<RetrievedContext> lorebookResults = hybridLorebookRecall(ctx.playerInput, location, worldVersion, docs);
        ctx.retrievedContexts.addAll(lorebookResults);

        ctx.retrievedContexts = rerankWithDiversity(ctx.retrievedContexts);

        log.debug("[{}] traceId={} retrieved={}", name(), ctx.traceId, ctx.retrievedContexts.size());
        next.handle(ctx);
    }

    private List<RetrievedContext> rerankWithDiversity(List<RetrievedContext> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }

        // 1) source+id 去重，保留高分
        Map<String, RetrievedContext> bySourceAndId = new LinkedHashMap<>();
        for (RetrievedContext rc : raw) {
            String key = canonicalSource(rc.source()) + "::" + (rc.id() == null ? "na" : rc.id());
            RetrievedContext existing = bySourceAndId.get(key);
            if (existing == null || rc.score() > existing.score()) {
                bySourceAndId.put(key, rc);
            }
        }

        List<RetrievedContext> deduped = bySourceAndId.values().stream()
                .sorted(Comparator.comparingDouble(RetrievedContext::score).reversed())
                .toList();

        // 2) 文本近重复去重（长度+前缀签名）
        List<RetrievedContext> textUnique = new ArrayList<>();
        Set<String> textSignatures = new HashSet<>();
        for (RetrievedContext rc : deduped) {
            String sign = textSignature(rc.text());
            if (textSignatures.add(sign)) {
                textUnique.add(rc);
            }
        }

        // 3) 多样性约束：先确保不同来源至少各取一条，再按分数补齐
        List<RetrievedContext> selected = new ArrayList<>();
        Set<String> selectedKey = new HashSet<>();
        Map<String, Integer> sourceCount = new HashMap<>();

        for (RetrievedContext rc : textUnique) {
            String source = canonicalSource(rc.source());
            if (sourceCount.getOrDefault(source, 0) == 0) {
                if (acceptByQuota(sourceCount, source)) {
                    selected.add(rc);
                    selectedKey.add(uniqueKey(rc));
                    sourceCount.put(source, sourceCount.getOrDefault(source, 0) + 1);
                }
            }
        }

        for (RetrievedContext rc : textUnique) {
            if (selected.size() >= FINAL_TOP_K) {
                break;
            }
            if (selectedKey.contains(uniqueKey(rc))) {
                continue;
            }
            String source = canonicalSource(rc.source());
            if (!acceptByQuota(sourceCount, source)) {
                continue;
            }
            selected.add(rc);
            selectedKey.add(uniqueKey(rc));
            sourceCount.put(source, sourceCount.getOrDefault(source, 0) + 1);
        }

        return selected;
    }

    private List<RetrievedContext> hybridLorebookRecall(String playerInput, String location, String worldVersion, List<Document> vectorDocs) {
        Map<String, LorebookCandidate> merged = new HashMap<>();

        // 关键词召回（高精确）
        List<LorebookEntry> keywordHits = lorebookRepo.findTopByKeywordAndVersion(playerInput, worldVersion, TOP_K);
        if (keywordHits.isEmpty()) {
            keywordHits = lorebookRepo.findTopByKeywordAndVersion(location, worldVersion, TOP_K);
        }
        for (int i = 0; i < keywordHits.size(); i++) {
            LorebookEntry hit = keywordHits.get(i);
            double rankScore = Math.max(0.55, 1.0 - i * 0.18);
            double priorityScore = normalizePriority(hit.getPriority());
            double keywordScore = Math.min(1.0, rankScore * 0.8 + priorityScore * 0.2);
            merged.computeIfAbsent(hit.getEntryId(), id -> new LorebookCandidate(id, hit.getBody()))
                    .keywordScore = keywordScore;
        }

        // 向量召回（高召回）
        for (Document doc : vectorDocs) {
            if (!isLorebookDoc(doc)) {
                continue;
            }
            String lorebookId = resolveLorebookId(doc);
            if (lorebookId == null || lorebookId.isBlank()) {
                continue;
            }
            LorebookCandidate candidate = merged.computeIfAbsent(
                    lorebookId,
                    id -> new LorebookCandidate(id, doc.getText())
            );
            candidate.vectorScore = Math.max(candidate.vectorScore, extractVectorScore(doc));
            if (candidate.text == null || candidate.text.isBlank()) {
                candidate.text = doc.getText();
            }
        }

        if (merged.isEmpty()) {
            return List.of();
        }

        List<LorebookCandidate> ranked = new ArrayList<>(merged.values());
        ranked.forEach(c -> c.fusedScore = fuseScore(c.vectorScore, c.keywordScore));
        ranked.sort(Comparator.comparingDouble((LorebookCandidate c) -> c.fusedScore).reversed());

        return ranked.stream()
                .limit(TOP_K)
                .map(c -> new RetrievedContext("lorebook", c.id, safeText(c.text), c.fusedScore))
                .toList();
    }

    private double normalizePriority(Integer priority) {
        int p = priority == null ? 50 : Math.max(0, Math.min(100, priority));
        return p / 100.0;
    }

    private double fuseScore(double vectorScore, double keywordScore) {
        if (vectorScore <= 0 && keywordScore <= 0) {
            return 0.60;
        }
        if (vectorScore <= 0) {
            return Math.max(0.52, keywordScore * 0.85);
        }
        if (keywordScore <= 0) {
            return Math.max(0.52, vectorScore * 0.90);
        }
        return W_VECTOR * vectorScore + W_KEYWORD * keywordScore;
    }

    private boolean isLorebookDoc(Document doc) {
        Object source = doc.getMetadata().get("source");
        Object type = doc.getMetadata().get("doc_type");
        String sourceText = source == null ? "" : String.valueOf(source).toLowerCase(Locale.ROOT);
        String typeText = type == null ? "" : String.valueOf(type).toLowerCase(Locale.ROOT);
        return sourceText.contains("lore") || typeText.contains("lore");
    }

    private String resolveLorebookId(Document doc) {
        Object refId = doc.getMetadata().get("ref_id");
        Object sourceId = doc.getMetadata().get("source_id");
        Object entryId = doc.getMetadata().get("entry_id");
        return List.of(refId, sourceId, entryId, doc.getId())
                .stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .filter(v -> !v.isBlank())
                .findFirst()
                .orElse(null);
    }

    private double extractVectorScore(Document doc) {
        Object score = doc.getMetadata().get("score");
        if (score instanceof Number n) {
            return clamp(n.doubleValue());
        }
        Object distance = doc.getMetadata().get("distance");
        if (distance instanceof Number n) {
            return clamp(1.0 - n.doubleValue());
        }
        return 0.68;
    }

    private String safeText(String text) {
        if (text == null || text.isBlank()) {
            return "缺少条目正文，保留占位信息。";
        }
        return text;
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private String canonicalSource(String source) {
        if (source == null || source.isBlank()) {
            return "vector";
        }
        String s = source.toLowerCase(Locale.ROOT);
        if (s.contains("event")) {
            return "event_card";
        }
        if (s.contains("lore")) {
            return "lorebook";
        }
        if (s.contains("memory_l0")) {
            return "memory_l0";
        }
        if (s.contains("memory_l1")) {
            return "memory_l1";
        }
        return "vector";
    }

    private boolean acceptByQuota(Map<String, Integer> sourceCount, String source) {
        int quota = switch (source) {
            case "event_card" -> 2;
            case "lorebook" -> 3;
            case "memory_l0" -> 2;
            case "memory_l1" -> 1;
            default -> 2;
        };
        return sourceCount.getOrDefault(source, 0) < quota;
    }

    private String uniqueKey(RetrievedContext rc) {
        return canonicalSource(rc.source()) + "::" + rc.id();
    }

    private String textSignature(String text) {
        String t = safeText(text).replaceAll("\\s+", " ").trim();
        int max = Math.min(60, t.length());
        return t.substring(0, max).toLowerCase(Locale.ROOT) + "#" + t.length();
    }

    private static final class LorebookCandidate {
        private final String id;
        private String text;
        private double vectorScore;
        private double keywordScore;
        private double fusedScore;

        private LorebookCandidate(String id, String text) {
            this.id = id;
            this.text = text;
        }
    }
}

