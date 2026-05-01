package com.doomsday.game.world;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "lorebook_entry")
public class LorebookEntry {

    @Id
    @Column(name = "entry_id")
    private String entryId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "tags_json", columnDefinition = "jsonb")
    private String tagsJson;

    @Column(nullable = false)
    private int priority;

    @Column(nullable = false)
    private String version;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public LorebookEntry() {}

    public String getEntryId() { return entryId; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getTagsJson() { return tagsJson; }
    public int getPriority() { return priority; }
    public String getVersion() { return version; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
