package com.doomsday.game.agent.impl;

import com.doomsday.game.agent.AgentChain;
import com.doomsday.game.agent.AgentHandler;
import com.doomsday.game.agent.TurnContext;
import com.doomsday.game.agent.TurnContext.RetrievedContext;
import com.doomsday.game.world.EventCardRepository;
import com.doomsday.game.world.LorebookEntryRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

/**
 * Retrieval Agent：多路召回，为 Plot 生成提供世界观证据。
 *
 * 召回顺序（优先级从高到低）：
 *   1. VectorStore 语义相似度（Spring AI PgVectorStore + DashScope text-embedding-v3）
 *   2. JPA 事件卡（按当前位置标签精确匹配）
 *   3. JPA Lorebook（关键词 ILIKE，失败时回退 location 匹配）
 *   4. L0 Rolling Memory（由 TurnOrchestrator 在链路开始时注入，此处只做扩充标记）
 */
@Component
public class RetrievalAgent implements AgentHandler {

    private static final Logger log = LoggerFactory.getLogger(RetrievalAgent.class);
    private static final int TOP_K = 3;
    private static final double SIMILARITY_THRESHOLD = 0.55;

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
        // --- 1. 语义向量检索（Spring AI VectorStore）---
        try {
            List<org.springframework.ai.document.Document> docs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(ctx.playerInput)
                            .topK(TOP_K)
                            .similarityThreshold(SIMILARITY_THRESHOLD)
                            .build()
            );
            docs.forEach(doc -> {
                String source = (String) doc.getMetadata().getOrDefault("source", "vector");
                double score = (Double) doc.getMetadata().getOrDefault("distance", 0.75);
                ctx.retrievedContexts.add(new RetrievedContext(source, doc.getId(), doc.getText(), score));
            });
        } catch (Exception e) {
            log.warn("[{}] traceId={} vector search failed: {}", name(), ctx.traceId, e.getMessage());
        }

        // --- 2. JPA 事件卡（位置标签精确匹配）---
        String location = ctx.session.getLocation();
        eventCardRepo.findTopByLocationTag(location, TOP_K)
                .forEach(ec -> ctx.retrievedContexts.add(
                        new RetrievedContext("event_card", ec.getEventId(), ec.getTriggerJson(), 0.85)
                ));

        // --- 3. JPA Lorebook（关键词 → 位置兜底）---
        List<RetrievedContext> lorebookResults = lorebookRepo
                .findTopByKeyword(ctx.playerInput, TOP_K)
                .stream()
                .map(lb -> new RetrievedContext("lorebook", lb.getEntryId(), lb.getBody(), 0.75))
                .toList();

        if (lorebookResults.isEmpty()) {
            lorebookResults = lorebookRepo
                    .findTopByKeyword(location, TOP_K)
                    .stream()
                    .map(lb -> new RetrievedContext("lorebook", lb.getEntryId(), lb.getBody(), 0.65))
                    .toList();
        }
        ctx.retrievedContexts.addAll(lorebookResults);

        log.debug("[{}] traceId={} retrieved={}", name(), ctx.traceId, ctx.retrievedContexts.size());
        next.handle(ctx);
    }
}

