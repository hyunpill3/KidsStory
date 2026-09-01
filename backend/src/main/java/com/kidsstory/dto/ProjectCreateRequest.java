package com.kidsstory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record ProjectCreateRequest(
        String storyPrompt,
        @NotNull @Valid ProjectOptions options,
        String captchaToken) {
}
