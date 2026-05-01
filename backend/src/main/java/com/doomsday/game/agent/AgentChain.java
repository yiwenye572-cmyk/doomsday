package com.doomsday.game.agent;

import java.util.List;

/**
 * 责任链执行器，依次调用 AgentHandler 列表。
 * 设计为不可变，线程安全。
 */
public final class AgentChain {

    private final List<AgentHandler> handlers;
    private final int index;

    public AgentChain(List<AgentHandler> handlers) {
        this(handlers, 0);
    }

    private AgentChain(List<AgentHandler> handlers, int index) {
        this.handlers = handlers;
        this.index = index;
    }

    public void handle(TurnContext ctx) {
        if (ctx.aborted || index >= handlers.size()) {
            return;
        }
        AgentHandler current = handlers.get(index);
        AgentChain next = new AgentChain(handlers, index + 1);
        current.handle(ctx, next);
    }
}
