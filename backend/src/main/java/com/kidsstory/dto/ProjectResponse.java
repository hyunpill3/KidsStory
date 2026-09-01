package com.kidsstory.dto;

import com.kidsstory.entity.Plan;
import com.kidsstory.entity.Project;
import com.kidsstory.entity.ProjectStatus;
import com.kidsstory.entity.Video;
import com.kidsstory.entity.VideoStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        String title,
        String storyPrompt,
        String generatedStory,
        Plan plan,
        ProjectOptions options,
        ProjectStatus status,
        int progress,
        List<ProjectImageResponse> images,
        List<SceneResponse> scenes,
        String videoUrl,
        Instant expiresAt,
        Instant createdAt,
        Instant updatedAt) {

    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getTitle(),
                project.getStoryPrompt(),
                project.getGeneratedStory(),
                project.getPlan(),
                new ProjectOptions(
                        project.getAgeGroup(),
                        project.getVideoLength(),
                        project.getStyle(),
                        project.getVoice(),
                        project.getLanguage()),
                project.getStatus(),
                project.getProgress(),
                project.getImages().stream().map(ProjectImageResponse::from).toList(),
                project.getScenes().stream().map(SceneResponse::from).toList(),
                latestVideoUrl(project),
                project.getExpiresAt(),
                project.getCreatedAt(),
                project.getUpdatedAt());
    }

    static String latestVideoUrl(Project project) {
        // Newest-first, matching the video player showing the most recent completed render.
        return project.getVideosNewestFirst().stream()
                .filter(v -> v.getStatus() == VideoStatus.COMPLETED && v.getUrl() != null)
                .map(Video::getUrl)
                .findFirst()
                .orElse(null);
    }
}
