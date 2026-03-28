package com.rabbit.common.enums;

public enum ProjectUserRole {
    MASTER("master"),
    SLAVE("slave");

    private final String value;

    ProjectUserRole(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ProjectUserRole fromValue(String value) {
        for (ProjectUserRole role : values()) {
            if (role.value.equals(value)) return role;
        }
        throw new IllegalArgumentException("Unknown ProjectUserRole: " + value);
    }
}