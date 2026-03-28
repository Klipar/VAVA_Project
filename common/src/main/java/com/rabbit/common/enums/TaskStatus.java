package com.rabbit.common.enums;

public enum TaskStatus {
    BACKLOG("backlog"),
    ASSIGNED("assigned"),
    IN_PROGRESS("in_progress"),
    READY("ready"),
    IN_REVIEW("in_review"),
    DONE("done");

    private final String value;

    TaskStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static TaskStatus fromValue(String value) {
        for (TaskStatus status : values()) {
            if (status.value.equals(value)) return status;
        }
        throw new IllegalArgumentException("Unknown TaskStatus: " + value);
    }
}