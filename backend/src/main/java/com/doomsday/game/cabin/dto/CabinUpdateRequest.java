package com.doomsday.game.cabin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

public class CabinUpdateRequest {

    @NotBlank
    private String idempotencyKey;

    @Min(0)
    private long expectedVersion;

    @NotNull
    private List<Map<String, Object>> changes; // [{"op":"move","itemId":"..","payload":{...}}]

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public long getExpectedVersion() { return expectedVersion; }
    public void setExpectedVersion(long expectedVersion) { this.expectedVersion = expectedVersion; }

    public List<Map<String, Object>> getChanges() { return changes; }
    public void setChanges(List<Map<String, Object>> changes) { this.changes = changes; }
}
