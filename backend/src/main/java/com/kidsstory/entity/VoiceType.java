package com.kidsstory.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum VoiceType {
    MALE("male"), FEMALE("female"), CALM_BEDTIME("calm_bedtime");

    private final String value;

    VoiceType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static VoiceType fromValue(String value) {
        for (VoiceType voice : values()) {
            if (voice.value.equals(value)) return voice;
        }
        throw new IllegalArgumentException("Unknown VoiceType: " + value);
    }
}
