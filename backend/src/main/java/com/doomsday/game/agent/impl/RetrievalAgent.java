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
import org.springframework.stereotype.Component;

/**
 * Retrieval Agent：多路召回事件卡、Lorebook、规则。
 *
 * P1 实现为关键词 + 标签过滤召回（无向量模型）。
 * 后续接入 pgvector 相似度检索时只需扩展此类，不改动链路。
 */
@Component
public class RetrievalAgent implements AgentHandler {

    private static final Logger log = LoggerFactory.getLogger(RetrievalAgent.class);
    private static final int TOP_K = 3;

    private final EventCardRepository eventCardRepo;
    private final LorebookEntryRepository lorebookRepo;

    public RetrievalAgent(EventCardRepository eventCardRepo,
                          LorebookEntryRepository lorebookRepo) {
        this.eventCardRepo = eventCardRepo;
        this.lorebookRepo = lorebookRepo;
    }

    @Override
    public String name() {
        return "RetrievalAgent";
    }

    @Override
    public void handle(TurnContext ctx, AgentChain next) {
        // 事件卡：按当前地点标签召回
        String locationTag = ctx.session.getLocation();
        List<RetrievedContext> eventResults = eventCardRepo
                .findTopByLocationTag(locationTag, TOP_K)
                .stream()
                .map(ec -> new RetrievedContext("event_card", ec.getEventId(),
                        ec.getTriggerJson(), 0.85))
                .toList();

        // Lorebook：按意图关键词召回
        List<RetrievedContext> lorebookResults = lorebookRepo
                .findTopByKeyword(ctx.playerInput, TOP_K)
                .stream()
                .map(lb -> new RetrievedContext("lorebook", lb.getEntryId(),
                        lb.getBody(), 0.75))
                .toList();

        ctx.retrievedContexts.addAll(eventResults);
        ctx.retrievedContexts.addAll(lorebookResults);

        log.debug("[{}] traceId={} retrieved={}", name(), ctx.traceId, ctx.retrievedContexts.size());

        next.handle(ctx);
    }
}
