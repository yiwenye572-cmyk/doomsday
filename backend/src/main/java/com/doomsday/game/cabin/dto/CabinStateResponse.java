package com.doomsday.game.cabin.dto;

public class CabinStateResponse {

    private String sessionId;
    private String stateData;

    public CabinStateResponse(String sessionId, String stateData) {
        this.sessionId = sessionId;
        this.stateData = stateData;
    }

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