package com.kidsstory.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum VisualStyle {
    THREE_D_CUTE("3d_cute"), STORYBOOK("storybook"), WATERCOLOR("watercolor"), CARTOON("cartoon");

    private final String value;

    VisualStyle(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static VisualStyle fromValue(String value) {
        for (VisualStyle style : values()) {
            if (style.value.equals(value)) return style;
        }
        throw new IllegalArgumentException("Unknown VisualStyle: " + value);
    }
}
