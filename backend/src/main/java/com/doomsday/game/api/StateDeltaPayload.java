package com.doomsday.game.api;

import java.util.List;

public record StateDeltaPayload(
        int stamina,
        int noise,
        List<String> flagsAdded
) {}
