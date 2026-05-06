package com.doomsday.game.tool;

import com.doomsday.game.agent.TurnContext;
import com.doomsday.game.domain.GameSession;

public record ToolContext(
        String sessionId,
        String traceId,
        String callerAgent,
        GameSession session,
        TurnContext turnContext
) {
}
