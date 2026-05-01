package com.doomsday.game.world;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "event_card")
public class EventCard {

    @Id
    @Column(name = "event_id")
    private String eventId;

    @Column(name = "trigger_json", columnDefinition = "jsonb")
    private String triggerJson;

    @Column(name = "effect_json", columnDefinition = "jsonb")
    private String effectJson;

    @Column(name = "constraints_json", columnDefinition = "jsonb")
    private String constraintsJson;

    @Column(nullable = false)
    private String rarity;

    @Column(nullable = false)
    private String version;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public EventCard() {}

    public String getEventId() { return eventId; }
    public String getTriggerJson() { return triggerJson; }
    public String getEffectJson() { return effectJson; }
    public String getConstraintsJson() { return constraintsJson; }
    public String getRarity() { return rarity; }
    public String getVersion() { return version; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
