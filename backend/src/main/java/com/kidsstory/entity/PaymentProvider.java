package com.kidsstory.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PaymentProvider {
    STRIPE("stripe"), TOSS("toss"), KAKAOPAY("kakaopay");

    private final String value;

    PaymentProvider(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static PaymentProvider fromValue(String value) {
        for (PaymentProvider provider : values()) {
            if (provider.value.equals(value)) return provider;
        }
        throw new IllegalArgumentException("Unknown PaymentProvider: " + value);
    }
}
