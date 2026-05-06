package com.doomsday.game.worldfactory.dto;

public record GameWorldInitResponse(
        String worldVersion,
        String jobId,
        String status,
        String message
) {}
