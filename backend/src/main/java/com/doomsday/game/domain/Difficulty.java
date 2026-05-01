package com.doomsday.game.domain;

public enum Difficulty {
    SEEKER,
    SURVIVOR,
    HELL;

    public double[] challengeBand() {
        return switch (this) {
            case SEEKER -> new double[]{0.35, 0.55};
            case SURVIVOR -> new double[]{0.45, 0.70};
            case HELL -> new double[]{0.60, 0.82};
        };
    }
}
