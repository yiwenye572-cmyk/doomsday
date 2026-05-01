package com.doomsday.game.agent;

/**
 * Agent 责任链节点接口。
 * 每个 Agent 实现本接口，在 handle() 中读取/写入 TurnContext，
 * 然后调用 next.handle(ctx) 继续链路（或 abort）。
 */
public interface AgentHandler {

    /** 节点名称，用于日志与追踪 */
    String name();

    /**
     * 处理当前节点逻辑。
     * 实现类必须在结尾调用 next.handle(ctx)（若 ctx.aborted == false）。
     */
    void handle(TurnContext ctx, AgentChain next);
}
