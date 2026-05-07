package com.doomsday.game.archive.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "game_archive_session")
public class GameArchiveSession {

    @Id
    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(name = "world_version")
    private String worldVersion;

    @Column(nullable = false)
    private String difficulty;

    @Column(name = "latest_turn", nullable = false)
    private int latestTurn;

    @Column(name = "latest_version", nullable = false)
    private long latestVersion;

    @Column(name = "day_index", nullable = false)
    private int dayIndex;

    @Column(name = "turn_in_day", nullable = false)
    private int turnInDay;

    @Column(name = "turns_per_day_target", nullable = false)
    private int turnsPerDayTarget;

    @Column(name = "time_phase", nullable = false)
    private String timePhase;

    @Column(name = "state_json", nullable = false, columnDefinition = "TEXT")
    private String stateJson;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getWorldVersion() {
        return worldVersion;
    }

    public void setWorldVersion(String worldVersion) {
        this.worldVersion = worldVersion;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public int getLatestTurn() {
        return latestTurn;
    }

    public void setLatestTurn(int latestTurn) {
        this.latestTurn = latestTurn;
    }

    public long getLatestVersion() {
        return latestVersion;
    }

    public void setLatestVersion(long latestVersion) {
        this.latestVersion = latestVersion;
    }

    public int getDayIndex() {
        return dayIndex;
    }

    public void setDayIndex(int dayIndex) {
        this.dayIndex = dayIndex;
    }

    public int getTurnInDay() {
        return turnInDay;
    }

    public void setTurnInDay(int turnInDay) {
        this.turnInDay = turnInDay;
    }

    public int getTurnsPerDayTarget() {
        return turnsPerDayTarget;
    }

    public void setTurnsPerDayTarget(int turnsPerDayTarget) {
        this.turnsPerDayTarget = turnsPerDayTarget;
    }

    public String getTimePhase() {
        return timePhase;
    }

    public void setTimePhase(String timePhase) {
        this.timePhase = timePhase;
    }

    public String getStateJson() {
        return stateJson;
    }

    public void setStateJson(String stateJson) {
        this.stateJson = stateJson;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
