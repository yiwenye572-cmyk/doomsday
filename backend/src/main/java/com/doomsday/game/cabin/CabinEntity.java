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

    /** 乐观锁版本号，Redis Lua CAS 与数据库均依赖此字段 */
    @Column(nullable = false)
    private long version = 0L;

    @Column(name = "state_data", columnDefinition = "text")
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

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
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