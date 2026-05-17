package com.doomsday.game.cabin.dto;

public class CabinUpdateResponse {
    private String sessionId;
    private long newVersion;
    private String stateData;
    private boolean conflict;
    private String conflictMessage;

    public CabinUpdateResponse(String sessionId, long newVersion, String stateData) {
        this.sessionId = sessionId;
        this.newVersion = newVersion;
        this.stateData = stateData;
        this.conflict = false;
    }

    /** 409 冲突时使用此工厂方法 */
    public static CabinUpdateResponse conflict(String sessionId, long currentVersion, String currentState) {
        CabinUpdateResponse r = new CabinUpdateResponse(sessionId, currentVersion, currentState);
        r.conflict = true;
        r.conflictMessage = "Version conflict: expected version does not match. Please refresh and retry.";
        return r;
    }

    public String getSessionId() { return sessionId; }
    public long getNewVersion() { return newVersion; }
    public String getStateData() { return stateData; }
    public boolean isConflict() { return conflict; }
    public String getConflictMessage() { return conflictMessage; }
}
