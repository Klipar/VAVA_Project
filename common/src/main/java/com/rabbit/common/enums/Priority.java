package com.rabbit.common.enums;

public enum Priority {
    TRIVIAL(1),
    LOW(2),
    MEDIUM(3),
    HIGH(4),
    CRITICAL(5);

    private final int level;

    Priority(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    public static Priority fromLevel(int level) {
        for (Priority p : values()) {
            if (p.level == level) return p;
        }
        throw new IllegalArgumentException("Unknown Priority level: " + level);
    }
}