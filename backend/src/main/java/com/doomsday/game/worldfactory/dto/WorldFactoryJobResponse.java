package com.doomsday.game.worldfactory.dto;

public record WorldFactoryJobResponse(
        String jobId,
        String worldVersion,
        String sourceType,
        String status,
        int progress,
        String stage,
        String errorMessage
) {}
