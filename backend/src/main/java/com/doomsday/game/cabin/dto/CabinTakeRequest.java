package com.doomsday.game.cabin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class CabinTakeRequest {

    @NotBlank
    private String idempotencyKey;

    @NotEmpty
    private List<String> itemIds;

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public List<String> getItemIds() { return itemIds; }
    public void setItemIds(List<String> itemIds) { this.itemIds = itemIds; }
}
