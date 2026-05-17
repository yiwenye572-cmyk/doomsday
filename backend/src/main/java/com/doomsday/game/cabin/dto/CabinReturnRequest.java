package com.doomsday.game.cabin.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

public class CabinReturnRequest {

    @NotBlank
    private String idempotencyKey;

    /** 归来后捡到的物品列表，每项为物品 metadata Map */
    private List<Map<String, Object>> foundItems;

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public List<Map<String, Object>> getFoundItems() { return foundItems; }
    public void setFoundItems(List<Map<String, Object>> foundItems) { this.foundItems = foundItems; }
}
