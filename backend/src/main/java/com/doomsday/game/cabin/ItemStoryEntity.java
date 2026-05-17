package com.doomsday.game.cabin;

import jakarta.persistence.*;

/**
 * 物品叙事任务实体（对应 item_story 表）
 * 状态机：PENDING → RUNNING → DONE / FAILED
 */
@Entity
@Table(name = "item_story",
       uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "item_id"}))
public class ItemStoryEntity {

    public enum Status { PENDING, RUNNING, DONE, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, length = 128)
    private String sessionId;

    @Column(name = "item_id", nullable = false, length = 128)
    private String itemId;

    @Column(name = "item_type", length = 64)
    private String itemType;

    @Lob
    @Column(name = "item_metadata")
    private String itemMetadata;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.PENDING;

    @Lob
    @Column(name = "story_text")
    private String storyText;

    @Lob
    @Column(name = "rag_citations")
    private String ragCitations;

    @Column(name = "error_message", length = 512)
    private String errorMessage;

    // ─── Getters / Setters ───────────────────────────────────────────────

    public Long getId() { return id; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }

    public String getItemMetadata() { return itemMetadata; }
    public void setItemMetadata(String itemMetadata) { this.itemMetadata = itemMetadata; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getStoryText() { return storyText; }
    public void setStoryText(String storyText) { this.storyText = storyText; }

    public String getRagCitations() { return ragCitations; }
    public void setRagCitations(String ragCitations) { this.ragCitations = ragCitations; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
