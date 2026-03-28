package com.rabbit.common.enums;

public enum ProjectStatus {
    ACTIVE("active"),
    COMPLETED("completed"),
    ARCHIVED("archived");

    private final String value;

    ProjectStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ProjectStatus fromValue(String value) {
        for (ProjectStatus status : values()) {
            if (status.value.equals(value)) return status;
        }
        throw new IllegalArgumentException("Unknown ProjectStatus: " + value);
    }
}