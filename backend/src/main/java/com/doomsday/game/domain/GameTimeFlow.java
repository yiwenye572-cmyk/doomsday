package com.doomsday.game.domain;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 游戏时间推进器：按天划分，单天 4-6 回合。
 */
public final class GameTimeFlow {

    private GameTimeFlow() {
    }

    public static void advanceAfterTurn(GameSession session) {
        if (session == null) {
            return;
        }
        int nextTurnInDay = session.getTurnInDay() + 1;
        int target = normalizeTarget(session.getTurnsPerDayTarget());

        if (nextTurnInDay > target) {
            session.setDayIndex(Math.max(1, session.getDayIndex()) + 1);
            session.setTurnsPerDayTarget(randomTurnsPerDay());
            session.setTurnInDay(1);
        } else {
            session.setTurnInDay(nextTurnInDay);
        }
        session.setTimePhase(resolvePhase(session.getTurnInDay(), session.getTurnsPerDayTarget()));
    }

    public static String resolvePhase(int turnInDay, int turnsPerDayTarget) {
        int target = normalizeTarget(turnsPerDayTarget);
        int inDay = Math.max(1, Math.min(turnInDay, target));
        int slot = (int) Math.floor(((double) (inDay - 1) * 4) / target);
        return switch (slot) {
            case 0 -> "MIDNIGHT";
            case 1 -> "DAY";
            case 2 -> "AFTERNOON";
            default -> "NIGHT";
        };
    }

    public static String phaseLabel(String phase) {
        if (phase == null || phase.isBlank()) {
            return "凌晨(00:00-06:00)";
        }
        return switch (phase) {
            case "MIDNIGHT" -> "凌晨(00:00-06:00)";
            case "DAY" -> "白天(06:00-12:00)";
            case "AFTERNOON" -> "下午(12:00-18:00)";
            case "NIGHT" -> "夜晚(18:00-24:00)";
            default -> "凌晨(00:00-06:00)";
        };
    }

    private static int normalizeTarget(int target) {
        if (target < 4 || target > 6) {
            return randomTurnsPerDay();
        }
        return target;
    }

    public static int randomTurnsPerDay() {
        return ThreadLocalRandom.current().nextInt(4, 7);
    }
}
