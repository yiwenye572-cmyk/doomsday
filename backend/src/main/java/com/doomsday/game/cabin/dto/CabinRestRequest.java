package com.doomsday.game.cabin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class CabinRestRequest {

    @NotBlank
    private String sessionId;

    @Min(1)
    private int durationHours;

    @NotBlank
    private String newTimeOfDay;

    // Getters and Setters

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public int getDurationHours() {
        return durationHours;
    }

    public void setDurationHours(int durationHours) {
        this.durationHours = durationHours;
    }

    public String getNewTimeOfDay() {
        return newTimeOfDay;
    }

    public void setNewTimeOfDay(String newTimeOfDay) {
        this.newTimeOfDay = newTimeOfDay;
    }
}