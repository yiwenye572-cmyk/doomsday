package com.doomsday.game.domain;

/**
 * L0 短时记忆条目：保留最近 N 回合关键信息，供 Plot 生成时回忆。
 */
public record TurnMemory(
        int turn,
        String playerInput,
        String narration,
        long timestamp
) {}
