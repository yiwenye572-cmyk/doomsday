package com.doomsday.game.diary;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DiarySummaryJobService {

    private final SessionIndexProvider sessionIndexProvider;
    private final GameDiaryService gameDiaryService;

    public DiarySummaryJobService(SessionIndexProvider sessionIndexProvider,
                                  GameDiaryService gameDiaryService) {
        this.sessionIndexProvider = sessionIndexProvider;
        this.gameDiaryService = gameDiaryService;
    }

    public int summarizeRecentActiveSessions(int limit) {
        List<String> sessions = sessionIndexProvider.findRecentSessionIds(limit);
        int created = 0;
        for (String sessionId : sessions) {
            if (gameDiaryService.maybeSummarizeByTurn(sessionId).created()) {
                created++;
            }
        }
        return created;
    }
}
