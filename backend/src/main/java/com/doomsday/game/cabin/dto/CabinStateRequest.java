package com.doomsday.game.cabin.dto;

import jakarta.validation.constraints.NotBlank;

public class CabinStateRequest {

    @NotBlank
    private String sessionId;

    @NotBlank
    private String stateData;

    // Getters and Setters

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getStateData() {
        return stateData;
    }

    public void setStateData(String stateData) {
        this.stateData = stateData;
    }
}