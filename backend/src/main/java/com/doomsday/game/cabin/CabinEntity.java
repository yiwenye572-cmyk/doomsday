package com.doomsday.game.cabin;

import jakarta.persistence.*;

@Entity
@Table(name = "cabin_state")
public class CabinEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sessionId;

    @Lob
    private String stateData;

    @Column(name = "player_stamina", nullable = false)
    private int playerStamina = 100;

    @Column(name = "time_of_day", nullable = false)
    private String timeOfDay = "morning";
    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public int getPlayerStamina() {
        return playerStamina;
    }

    public void setPlayerStamina(int playerStamina) {
        this.playerStamina = playerStamina;
    }

    public String getTimeOfDay() {
        return timeOfDay;
    }

    public void setTimeOfDay(String timeOfDay) {
        this.timeOfDay = timeOfDay;
    }
}