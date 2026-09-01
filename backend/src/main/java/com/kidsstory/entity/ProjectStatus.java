package com.kidsstory.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ProjectStatus {
    DRAFT("draft"),
    QUEUED("queued"),
    ANALYZING_PHOTOS("analyzing_photos"),
    GENERATING_STORY("generating_story"),
    GENERATING_SCENES("generating_scenes"),
    GENERATING_NARRATION("generating_narration"),
    COMPOSING_AUDIO("composing_audio"),
    RENDERING_VIDEO("rendering_video"),
    COMPLETED("completed"),
    FAILED("failed");

    private final String value;

    ProjectStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ProjectStatus fromValue(String value) {
        for (ProjectStatus status : values()) {
            if (status.value.equals(value)) return status;
        }
        throw new IllegalArgumentException("Unknown ProjectStatus: " + value);
    }
}
