package com.kidsstory.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CreditReason {
    SIGNUP_BONUS("signup_bonus"),
    PURCHASE("purchase"),
    VIDEO_GENERATION("video_generation"),
    REFUND("refund"),
    ADMIN_ADJUSTMENT("admin_adjustment");

    private final String value;

    CreditReason(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static CreditReason fromValue(String value) {
        for (CreditReason reason : values()) {
            if (reason.value.equals(value)) return reason;
        }
        throw new IllegalArgumentException("Unknown CreditReason: " + value);
    }
}
