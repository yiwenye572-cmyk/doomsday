package com.doomsday.game.diary.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "game_diary_l1")
public class DiaryEntryL1 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(name = "world_version")
    private String worldVersion;

    @Column(name = "from_turn", nullable = false)
    private int fromTurn;

    @Column(name = "to_turn", nullable = false)
    private int toTurn;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(name = "tags_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String tagsJson;

    @Column(nullable = false)
    private String source;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public DiaryEntryL1() {
    }

    public Long getId() {
        return id;
    }

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

    public int getFromTurn() {
        return fromTurn;
    }

    public void setFromTurn(int fromTurn) {
        this.fromTurn = fromTurn;
    }

    public int getToTurn() {
        return toTurn;
    }

    public void setToTurn(int toTurn) {
        this.toTurn = toTurn;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getTagsJson() {
        return tagsJson;
    }

    public void setTagsJson(String tagsJson) {
        this.tagsJson = tagsJson;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
