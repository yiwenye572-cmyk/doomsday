package com.doomsday.game.domain;

import com.doomsday.game.api.OptionPayload;
import com.doomsday.game.api.StateDeltaPayload;
import java.util.List;

public record ReplayTurn(
        int turn,
        String actionType,
        String inputText,
        String selectedOptionId,
        String selectedOptionText,
        String plotText,
        List<OptionPayload> options,
        StateDeltaPayload stateDelta,
        long timestamp
) {
}