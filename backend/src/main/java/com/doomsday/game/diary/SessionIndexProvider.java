package com.doomsday.game.diary;

import java.util.List;

public interface SessionIndexProvider {
    List<String> findRecentSessionIds(int limit);
}
