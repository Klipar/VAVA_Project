package com.rabbit.common.enums;

import lombok.Getter;

@Getter
public enum Priority {
    TRIVIAL(0, "#95a5a6"),
    LOW(1, "#27ae60"),
    MEDIUM(2, "#f1c40f"),
    HIGH(3, "#e67e22"),
    CRITICAL(4, "#e74c3c");

    private final int level;
    private final String color;

    Priority(int level, String color) {
        this.level = level;
        this.color = color;
    }

    public static Priority fromLevel(int level) {
        for (Priority p : values()) {
            if (p.level == level) return p;
        }
        return MEDIUM;
    }
}