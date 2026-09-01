package com.kidsstory.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AgeGroup {
    AGE_3_5("3-5"), AGE_6_8("6-8"), AGE_9_12("9-12");

    private final String value;

    AgeGroup(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static AgeGroup fromValue(String value) {
        for (AgeGroup group : values()) {
            if (group.value.equals(value)) return group;
        }
        throw new IllegalArgumentException("Unknown AgeGroup: " + value);
    }
}
