package com.doomsday.game.archive;

import com.doomsday.game.archive.model.GameArchiveEvent;
import com.doomsday.game.archive.model.GameArchiveSession;
import com.doomsday.game.archive.repo.GameArchiveEventRepository;
import com.doomsday.game.archive.repo.GameArchiveSessionRepository;
import com.doomsday.game.domain.GameSession;
import com.doomsday.game.domain.GameTimeFlow;
import com.doomsday.game.domain.ReplayTurn;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GameArchiveService {

    private final GameArchiveSessionRepository sessionRepository;
    private final GameArchiveEventRepository eventRepository;
    private final ObjectMapper objectMapper;

    public GameArchiveService(GameArchiveSessionRepository sessionRepository,
                              GameArchiveEventRepository eventRepository,
                              ObjectMapper objectMapper) {
        this.sessionRepository = sessionRepository;
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
    }

    public void persistSessionSnapshot(GameSession session) {
        if (session == null) {
            return;
        }
        GameArchiveSession snapshot = sessionRepository.findById(session.getSessionId())
                .orElseGet(GameArchiveSession::new);
        snapshot.setSessionId(session.getSessionId());
        snapshot.setWorldVersion(session.getWorldVersion());
        snapshot.setDifficulty(session.getDifficulty().name());
        snapshot.setLatestTurn(session.getTurn());
        snapshot.setLatestVersion(session.getVersion());
        snapshot.setDayIndex(session.getDayIndex());
        snapshot.setTurnInDay(session.getTurnInDay());
        snapshot.setTurnsPerDayTarget(session.getTurnsPerDayTarget());
        snapshot.setTimePhase(session.getTimePhase());
        snapshot.setStateJson(writeJson(session));
        OffsetDateTime now = OffsetDateTime.now();
        if (snapshot.getCreatedAt() == null) {
            snapshot.setCreatedAt(now);
        }
        snapshot.setUpdatedAt(now);
        sessionRepository.save(snapshot);
    }

    public void appendReplayEvent(GameSession session, ReplayTurn replayTurn) {
        if (session == null || replayTurn == null) {
            return;
        }
        GameArchiveEvent event = new GameArchiveEvent();
        event.setSessionId(session.getSessionId());
        event.setTurnNo(replayTurn.turn());
        event.setDayIndex(replayTurn.dayIndex());
        event.setTimePhase(replayTurn.timePhase());
        event.setActionType(replayTurn.actionType());
        event.setNarrative(buildNarrative(replayTurn));
        event.setStateDeltaJson(writeJson(replayTurn.stateDelta()));
        eventRepository.save(event);
    }

    public List<GameArchiveEvent> findReplayEvents(String sessionId, Integer fromTurn, Integer toTurn) {
        return eventRepository.findEvents(sessionId, fromTurn, toTurn, 600);
    }

    private String buildNarrative(ReplayTurn row) {
        StringBuilder text = new StringBuilder();
        text.append("第").append(Math.max(1, row.dayIndex())).append("天 · ")
                .append(GameTimeFlow.phaseLabel(row.timePhase())).append("\n");
        if (row.inputText() != null && !row.inputText().isBlank()) {
            text.append("你选择了：").append(row.inputText()).append("\n");
        }
        if (row.selectedOptionText() != null && !row.selectedOptionText().isBlank()) {
            text.append("执行动作：").append(row.selectedOptionText()).append("\n");
        }
        if (row.plotText() != null && !row.plotText().isBlank()) {
            text.append(row.plotText());
        }
        return text.toString().trim();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "{}";
        }
    }
}
