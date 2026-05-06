package com.doomsday.game.arbitration;

public record LayerOutcome(boolean pass, String code, String reason) {
    public static LayerOutcome pass(String code) {
        return new LayerOutcome(true, code, null);
    }

    public static LayerOutcome fail(String code, String reason) {
        return new LayerOutcome(false, code, reason);
    }
}
