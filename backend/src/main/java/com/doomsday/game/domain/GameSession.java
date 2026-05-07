package com.doomsday.game.domain;

import com.doomsday.game.api.OptionPayload;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.concurrent.ThreadLocalRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * 游戏会话领域模型，作为 Redis JSON 序列化单元存储。
 */
public class GameSession {

    private final String sessionId;
    private final Difficulty difficulty;
    private long version;
    private int turn;
    private int currentTurn;
    private int hp;
    private int stamina;
    private int infection;
    private String location;
    private String worldVersion;
    private List<String> inventory;
    private int comebackCardRemaining;
    private double challengeIndex;
    private List<OptionPayload> currentOptions;
    private int dayIndex;
    private int turnInDay;
    private int turnsPerDayTarget;
    private String timePhase;

    /** Jackson 反序列化入口 */
    @JsonCreator
    public GameSession(
            @JsonProperty("sessionId") String sessionId,
            @JsonProperty("difficulty") Difficulty difficulty,
            @JsonProperty("version") long version,
            @JsonProperty("turn") int turn,
            @JsonProperty("currentTurn") int currentTurn,
            @JsonProperty("hp") int hp,
            @JsonProperty("stamina") int stamina,
            @JsonProperty("infection") int infection,
            @JsonProperty("location") String location,
            @JsonProperty("worldVersion") String worldVersion,
            @JsonProperty("inventory") List<String> inventory,
            @JsonProperty("comebackCardRemaining") int comebackCardRemaining,
            @JsonProperty("challengeIndex") double challengeIndex,
            @JsonProperty("currentOptions") List<OptionPayload> currentOptions,
            @JsonProperty("dayIndex") Integer dayIndex,
            @JsonProperty("turnInDay") Integer turnInDay,
            @JsonProperty("turnsPerDayTarget") Integer turnsPerDayTarget,
            @JsonProperty("timePhase") String timePhase) {
        this.sessionId = sessionId;
        this.difficulty = difficulty;
        this.version = version;
        this.turn = turn;
        this.currentTurn = currentTurn;
        this.hp = hp;
        this.stamina = stamina;
        this.infection = infection;
        this.location = location;
        this.worldVersion = (worldVersion == null || worldVersion.isBlank()) ? "world_v1" : worldVersion;
        this.inventory = inventory != null ? new ArrayList<>(inventory) : new ArrayList<>();
        this.comebackCardRemaining = comebackCardRemaining;
        this.challengeIndex = challengeIndex;
        this.currentOptions = currentOptions != null ? new ArrayList<>(currentOptions) : new ArrayList<>();
        this.dayIndex = dayIndex == null || dayIndex <= 0 ? 1 : dayIndex;
        this.turnInDay = turnInDay == null || turnInDay <= 0 ? 1 : turnInDay;
        this.turnsPerDayTarget = turnsPerDayTarget == null || turnsPerDayTarget < 4
            ? randomTurnsPerDay()
            : turnsPerDayTarget;
        this.timePhase = (timePhase == null || timePhase.isBlank()) ? "MIDNIGHT" : timePhase;
    }

    /** 新建会话工厂方法 */
    public static GameSession create(String sessionId, Difficulty difficulty, String worldVersion) {
        return new GameSession(
                sessionId, difficulty,
                1L, 0, 0,
                100, 100, 0,
                "safe_house",
                worldVersion,
                new ArrayList<>(List.of("knife", "bandage")),
                1, 0.5,
                new ArrayList<>(),
                1,
                1,
                randomTurnsPerDay(),
                "MIDNIGHT"
        );
    }

    // ===== getters =====
    public String getSessionId() { return sessionId; }
    public Difficulty getDifficulty() { return difficulty; }
    public long getVersion() { return version; }
    public int getTurn() { return turn; }
    public int getCurrentTurn() { return currentTurn; }
    public int getHp() { return hp; }
    public int getStamina() { return stamina; }
    public int getInfection() { return infection; }
    public String getLocation() { return location; }
    public String getWorldVersion() { return worldVersion; }
    public List<String> getInventory() { return inventory; }
    public int getComebackCardRemaining() { return comebackCardRemaining; }
    public double getChallengeIndex() { return challengeIndex; }
    public List<OptionPayload> getCurrentOptions() { return currentOptions; }
    public int getDayIndex() { return dayIndex; }
    public int getTurnInDay() { return turnInDay; }
    public int getTurnsPerDayTarget() { return turnsPerDayTarget; }
    public String getTimePhase() { return timePhase; }

    // ===== setters =====
    public void setVersion(long version) { this.version = version; }
    public void setTurn(int turn) { this.turn = turn; }
    public void setCurrentTurn(int currentTurn) { this.currentTurn = currentTurn; }
    public void setHp(int hp) { this.hp = hp; }
    public void setStamina(int stamina) { this.stamina = stamina; }
    public void setInfection(int infection) { this.infection = infection; }
    public void setLocation(String location) { this.location = location; }
    public void setWorldVersion(String worldVersion) {
        this.worldVersion = (worldVersion == null || worldVersion.isBlank()) ? "world_v1" : worldVersion;
    }
    public void setInventory(List<String> inventory) { this.inventory = inventory; }
    public void setComebackCardRemaining(int comebackCardRemaining) { this.comebackCardRemaining = comebackCardRemaining; }
    public void setChallengeIndex(double challengeIndex) { this.challengeIndex = challengeIndex; }
    public void setCurrentOptions(List<OptionPayload> currentOptions) { this.currentOptions = currentOptions; }
    public void setDayIndex(int dayIndex) { this.dayIndex = dayIndex; }
    public void setTurnInDay(int turnInDay) { this.turnInDay = turnInDay; }
    public void setTurnsPerDayTarget(int turnsPerDayTarget) { this.turnsPerDayTarget = turnsPerDayTarget; }
    public void setTimePhase(String timePhase) { this.timePhase = timePhase; }

    private static int randomTurnsPerDay() {
        return ThreadLocalRandom.current().nextInt(4, 7);
    }
}
