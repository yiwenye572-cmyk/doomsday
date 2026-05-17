package com.doomsday.game.cabin.dto;

public class CabinRestResponse {

    private String sessionId;
    private int updatedStamina;
    private String updatedTimeOfDay;

    public CabinRestResponse(String sessionId, int updatedStamina, String updatedTimeOfDay) {
        this.sessionId = sessionId;
        this.updatedStamina = updatedStamina;
        this.updatedTimeOfDay = updatedTimeOfDay;
    }

    // Getters and Setters

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public int getUpdatedStamina() {
        return updatedStamina;
    }

    public void setUpdatedStamina(int updatedStamina) {
        this.updatedStamina = updatedStamina;
    }

    public String getUpdatedTimeOfDay() {
        return updatedTimeOfDay;
    }

    public void setUpdatedTimeOfDay(String updatedTimeOfDay) {
        this.updatedTimeOfDay = updatedTimeOfDay;
    }
}