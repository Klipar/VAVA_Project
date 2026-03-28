package com.rabbit.common.enums;

public enum UserRole {
    MANAGER("manager"),
    TEAM_LEADER("team_leader"),
    WORKER("worker");

    private final String value;

    UserRole(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static UserRole fromValue(String value) {
        for (UserRole role : values()) {
            if (role.value.equals(value)) return role;
        }
        throw new IllegalArgumentException("Unknown UserRole: " + value);
    }
}