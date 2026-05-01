package com.doomsday.game.api;

import java.util.List;

public record SubmitTurnResponse(
        int turn,
        long newVersion,
        PlotPayload plot,
        List<OptionPayload> options,
        DifficultyDeltaPayload difficultyDelta,
        StateDeltaPayload stateDelta
) {}
