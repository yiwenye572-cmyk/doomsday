package com.doomsday.game.api;

import java.util.List;

public record PlotPayload(
        String text,
        List<String> citations,
        double confidence
) {}
