package com.kidsstory.dto;

import com.kidsstory.entity.ProjectImage;
import java.util.UUID;

public record ProjectImageResponse(UUID id, String url, String thumbnailUrl, int order) {

    public static ProjectImageResponse from(ProjectImage image) {
        return new ProjectImageResponse(
                image.getId(), image.getUrl(), image.getThumbnailUrl(), image.getDisplayOrder());
    }
}
