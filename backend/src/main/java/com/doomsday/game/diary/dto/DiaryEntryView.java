package com.doomsday.game.diary.dto;

import java.util.List;

public record DiaryEntryView(
        String level,
        int fromTurn,
        int toTurn,
        int dayIndex,
        String timePhase,
        String timePhaseLabel,
        String summary,
        List<String> tags,
        long timestamp
) {
}
