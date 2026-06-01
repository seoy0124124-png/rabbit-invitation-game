package com.fairytale.mystery.model;

import java.time.Instant;

public class GameState {
    private GamePhase phase = GamePhase.WAITING;
    private int timerSeconds = 600;
    private Instant timerEndsAt;
    private boolean timerRunning;

    public GamePhase getPhase() {
        return phase;
    }

    public void setPhase(GamePhase phase) {
        this.phase = phase;
    }

    public int getTimerSeconds() {
        return timerSeconds;
    }

    public void setTimerSeconds(int timerSeconds) {
        this.timerSeconds = Math.max(0, timerSeconds);
    }

    public Instant getTimerEndsAt() {
        return timerEndsAt;
    }

    public void setTimerEndsAt(Instant timerEndsAt) {
        this.timerEndsAt = timerEndsAt;
    }

    public boolean isTimerRunning() {
        return timerRunning;
    }

    public void setTimerRunning(boolean timerRunning) {
        this.timerRunning = timerRunning;
    }

}
