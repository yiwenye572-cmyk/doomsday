package com.doomsday.game.archive.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "game_archive_event")
public class GameArchiveEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(name = "turn_no", nullable = false)
    private int turnNo;

    @Column(name = "day_index", nullable = false)
    private int dayIndex;

    @Column(name = "time_phase", nullable = false)
    private String timePhase;

    @Column(name = "action_type", nullable = false)
    private String actionType;

    @Column(name = "narrative", nullable = false, columnDefinition = "TEXT")
    private String narrative;

    @Column(name = "state_delta_json", nullable = false, columnDefinition = "TEXT")
    private String stateDeltaJson;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public Long getId() {
        return id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public int getTurnNo() {
        return turnNo;
    }

    public void setTurnNo(int turnNo) {
        this.turnNo = turnNo;
    }

    public int getDayIndex() {
        return dayIndex;
    }

    public void setDayIndex(int dayIndex) {
        this.dayIndex = dayIndex;
    }

    public String getTimePhase() {
        return timePhase;
    }

    public void setTimePhase(String timePhase) {
        this.timePhase = timePhase;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getNarrative() {
        return narrative;
    }

    public void setNarrative(String narrative) {
        this.narrative = narrative;
    }

    public String getStateDeltaJson() {
        return stateDeltaJson;
    }

    public void setStateDeltaJson(String stateDeltaJson) {
        this.stateDeltaJson = stateDeltaJson;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
