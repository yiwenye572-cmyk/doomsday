package com.doomsday.game.diary.repo;

import com.doomsday.game.diary.model.DiaryEntryL1;
import com.doomsday.game.diary.model.DiaryEntryL2;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class GameDiaryRepository {

    private final GameDiaryL1Repository l1Repository;
    private final GameDiaryL2Repository l2Repository;

    public GameDiaryRepository(GameDiaryL1Repository l1Repository,
                               GameDiaryL2Repository l2Repository) {
        this.l1Repository = l1Repository;
        this.l2Repository = l2Repository;
    }

    public DiaryEntryL1 saveL1(DiaryEntryL1 entry) {
        return l1Repository.save(entry);
    }

    public DiaryEntryL2 saveL2(DiaryEntryL2 entry) {
        return l2Repository.save(entry);
    }

    public List<DiaryEntryL1> findL1(String sessionId, Integer fromTurn, Integer toTurn, int limit) {
        return l1Repository.findEntries(sessionId, fromTurn, toTurn, limit);
    }

    public List<DiaryEntryL2> findL2(String sessionId, Integer fromTurn, Integer toTurn, int limit) {
        return l2Repository.findEntries(sessionId, fromTurn, toTurn, limit);
    }

    public int findL1LastTurn(String sessionId) {
        return l1Repository.findLastSummarizedTurn(sessionId);
    }
}
