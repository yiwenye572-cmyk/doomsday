package com.doomsday.game.cabin.dto;

public class CabinStateResponse {

    private String sessionId;
    private long version;
    private String stateData;
    private int playerStamina;
    private String timeOfDay;

    public CabinStateResponse(String sessionId, long version, String stateData, int playerStamina, String timeOfDay) {
        this.sessionId = sessionId;
        this.version = version;
        this.stateData = stateData;
        this.playerStamina = playerStamina;
        this.timeOfDay = timeOfDay;
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }

    public String getStateData() { return stateData; }
    public void setStateData(String stateData) { this.stateData = stateData; }

    public int getPlayerStamina() { return playerStamina; }
    public void setPlayerStamina(int playerStamina) { this.playerStamina = playerStamina; }

    public String getTimeOfDay() { return timeOfDay; }
    public void setTimeOfDay(String timeOfDay) { this.timeOfDay = timeOfDay; }
}
