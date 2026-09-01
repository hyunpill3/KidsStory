package com.kidsstory.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SubscriptionStatus {
    ACTIVE("active"), TRIALING("trialing"), PAST_DUE("past_due"), CANCELED("canceled"), EXPIRED("expired");

    private final String value;

    SubscriptionStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static SubscriptionStatus fromValue(String value) {
        for (SubscriptionStatus status : values()) {
            if (status.value.equals(value)) return status;
        }
        throw new IllegalArgumentException("Unknown SubscriptionStatus: " + value);
    }
}
