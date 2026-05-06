package com.doomsday.game.diary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DiarySummaryScheduler {

    private static final Logger log = LoggerFactory.getLogger(DiarySummaryScheduler.class);

    private final DiarySummaryJobService diarySummaryJobService;
    private final boolean enabled;
    private final int sweepLimit;

    public DiarySummaryScheduler(DiarySummaryJobService diarySummaryJobService,
                                 @Value("${game.diary.summary.scheduler-enabled:true}") boolean enabled,
                                 @Value("${game.diary.summary.sweep-limit:50}") int sweepLimit) {
        this.diarySummaryJobService = diarySummaryJobService;
        this.enabled = enabled;
        this.sweepLimit = Math.max(1, sweepLimit);
    }

    @Scheduled(fixedDelayString = "${game.diary.summary.fixed-delay-ms:60000}")
    public void run() {
        if (!enabled) {
            return;
        }
        int created = diarySummaryJobService.summarizeRecentActiveSessions(sweepLimit);
        if (created > 0) {
            log.info("[DiarySummary] created {} summaries in this sweep", created);
        }
    }
}
