package com.doomsday.game.api;

import java.util.List;

public record ChooseOptionResponse(
        int turn,
        String selected,
        boolean applied,
        long newVersion,
        StateDeltaPayload stateDelta,
        PlotPayload plot,
        List<OptionPayload> options,
        DifficultyDeltaPayload difficultyDelta
) {}
