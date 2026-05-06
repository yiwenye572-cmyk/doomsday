package com.doomsday.game.worldfactory.dto;

import com.doomsday.game.worldfactory.WorldSourceType;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record WorldFactoryJobRequest(
        String worldVersion,
        @NotNull WorldSourceType sourceType,
        String content,
        boolean forceRebuild,
        Map<String, String> basicProfile
) {}
