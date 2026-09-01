package com.kidsstory.dto;

import com.kidsstory.entity.Scene;
import java.util.UUID;

public record SceneResponse(
        UUID id,
        int order,
        String narration,
        String visualDescription,
        String imageUrl,
        String videoUrl,
        String narrationAudioUrl,
        String mixedAudioUrl) {

    public static SceneResponse from(Scene scene) {
        return new SceneResponse(
                scene.getId(),
                scene.getDisplayOrder(),
                scene.getNarration(),
                scene.getVisualDescription(),
                scene.getImageUrl(),
                scene.getVideoUrl(),
                scene.getNarrationAudioUrl(),
                scene.getMixedAudioUrl());
    }
}
