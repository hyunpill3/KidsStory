package com.kidsstory.dto;

import com.kidsstory.entity.Project;
import com.kidsstory.entity.ProjectStatus;
import java.time.Instant;

public record ProjectStatusResponse(
        ProjectStatus status,
        int progress,
        String currentStep,
        String videoUrl,
        Instant expiresAt,
        String errorMessage) {

    public static ProjectStatusResponse from(Project project) {
        return new ProjectStatusResponse(
                project.getStatus(),
                project.getProgress(),
                project.getStatus().getValue(),
                ProjectResponse.latestVideoUrl(project),
                project.getExpiresAt(),
                project.getErrorMessage());
    }
}
