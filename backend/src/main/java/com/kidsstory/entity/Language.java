package com.kidsstory.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Language {
    KO("ko"), EN("en");

    private final String value;

    Language(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static Language fromValue(String value) {
        for (Language language : values()) {
            if (language.value.equals(value)) return language;
        }
        throw new IllegalArgumentException("Unknown Language: " + value);
    }
}
