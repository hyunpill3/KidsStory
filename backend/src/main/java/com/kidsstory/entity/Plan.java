package com.kidsstory.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Plan {
    FREE("free"), BASIC("basic"), PREMIUM("premium");

    private final String value;

    Plan(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static Plan fromValue(String value) {
        for (Plan plan : values()) {
            if (plan.value.equals(value)) return plan;
        }
        throw new IllegalArgumentException("Unknown Plan: " + value);
    }

    /** Ported for schema parity; not wired into any active flow (matches the Python original). */
    public static Plan resolveByPhotoCount(int count) {
        if (count <= 1) return FREE;
        if (count <= 3) return BASIC;
        return PREMIUM;
    }
}
