package com.kidsstory.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum VideoStatus {
    PROCESSING("processing"), COMPLETED("completed"), FAILED("failed");

    private final String value;

    VideoStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static VideoStatus fromValue(String value) {
        for (VideoStatus status : values()) {
            if (status.value.equals(value)) return status;
        }
        throw new IllegalArgumentException("Unknown VideoStatus: " + value);
    }
}
