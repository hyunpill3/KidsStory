package com.kidsstory.dto;

import com.kidsstory.entity.AgeGroup;
import com.kidsstory.entity.Language;
import com.kidsstory.entity.VisualStyle;
import com.kidsstory.entity.VoiceType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ProjectOptions(
        @NotNull AgeGroup ageGroup,
        @Positive int videoLength,
        @NotNull VisualStyle style,
        @NotNull VoiceType voice,
        @NotNull Language language) {
}
