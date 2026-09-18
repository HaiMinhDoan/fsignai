package com.sunmoon.backend.constant.enums;

public enum RiskLevel {
    LOW(80, 100), MEDIUM(60, 79), HIGH(40, 59), CRITICAL(0, 39);

    private final int min, max;
    RiskLevel(int min, int max) { this.min = min; this.max = max; }

    public static RiskLevel fromScore(int score) {
        for (RiskLevel level : values()) {
            if (score >= level.min && score <= level.max) return level;
        }
        throw new IllegalArgumentException("Điểm không hợp lệ: " + score);
    }
}
