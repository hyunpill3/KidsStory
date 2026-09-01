package com.kidsstory.service;

import com.kidsstory.config.AppProperties;
import com.kidsstory.dto.ProjectCreateRequest;
import com.kidsstory.dto.ProjectResponse;
import com.kidsstory.dto.ProjectStatusResponse;
import com.kidsstory.entity.Plan;
import com.kidsstory.entity.Project;
import com.kidsstory.entity.ProjectImage;
import com.kidsstory.entity.ProjectStatus;
import com.kidsstory.exception.ApiException;
import com.kidsstory.repository.ProjectRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Mirrors services/project_service.py. Each public method is transactional
 * end-to-end (fetch, mutate, and map to a response DTO all inside one
 * transaction) so lazily-loaded collections (images/scenes/videos) are
 * always resolved before the entity would otherwise detach - the DTO, not
 * the entity, is what leaves this class.
 */
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final StorageService storageService;
    private final AppProperties appProperties;

    @Transactional
    public ProjectResponse createProject(AnonIdentity identity, ProjectCreateRequest payload) {
        checkDailyLimit(identity.anonId(), identity.clientIp());

        String storyPrompt = payload.storyPrompt();
        String title = (storyPrompt != null && !storyPrompt.isBlank()) ? storyPrompt : "Untitled Story";
        if (title.length() > 60) {
            title = title.substring(0, 60);
        }

        Project project = new Project();
        project.setAnonId(identity.anonId());
        project.setClientIp(identity.clientIp());
        project.setTitle(title);
        project.setStoryPrompt(storyPrompt);
        project.setStatus(ProjectStatus.DRAFT);
        project.setPlan(Plan.FREE);
        project.setAgeGroup(payload.options().ageGroup());
        // Free tier is capped regardless of what the client sends.
        project.setVideoLength(Math.min(payload.options().videoLength(), appProperties.getMaxVideoLengthFree()));
        project.setStyle(payload.options().style());
        project.setVoice(payload.options().voice());
        project.setLanguage(payload.options().language());
        project.setExpiresAt(Instant.now().plus(Duration.ofHours(appProperties.getProjectTtlHours())));

        project = projectRepository.save(project);
        return ProjectResponse.from(project);
    }

    @Transactional(readOnly = true)
    public ProjectStatusResponse getStatus(UUID projectId, String anonId) {
        return ProjectStatusResponse.from(getProjectOrThrow(projectId, anonId));
    }

    @Transactional
    public ProjectResponse addImages(UUID projectId, String anonId, List<MultipartFile> files) {
        Project project = getProjectOrThrow(projectId, anonId);

        if (files == null || files.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Please upload at least one photo.");
        }

        int existingCount = project.getImages().size();
        if (existingCount + files.size() > appProperties.getMaxPhotosFree()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "The free plan allows up to " + appProperties.getMaxPhotosFree() + " photo(s) per video.");
        }

        for (MultipartFile file : files) {
            if (file.getSize() > appProperties.getMaxUploadBytes()) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "Each photo must be smaller than "
                                + (appProperties.getMaxUploadBytes() / (1024 * 1024))
                                + "MB.");
            }
        }

        int index = 0;
        for (MultipartFile file : files) {
            StorageService.ImageUrls urls = storageService.saveImage(project.getId(), file);
            ProjectImage image = new ProjectImage();
            image.setProject(project);
            image.setUrl(urls.url());
            image.setThumbnailUrl(urls.thumbnailUrl());
            image.setDisplayOrder(existingCount + index);
            project.getImages().add(image);
            index++;
        }

        project = projectRepository.save(project);
        return ProjectResponse.from(project);
    }

    /**
     * Marks a project QUEUED so the caller can hand it off to the async
     * pipeline. Deliberately does NOT trigger that pipeline itself - the
     * controller does so only after this transaction has committed, so the
     * background thread's own transaction is guaranteed to see the QUEUED
     * status and uploaded images rather than racing this one.
     */
    @Transactional
    public ProjectResponse markQueued(UUID projectId, String anonId) {
        Project project = getProjectOrThrow(projectId, anonId);
        if (project.getImages().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Please upload a photo first.");
        }

        project.setStatus(ProjectStatus.QUEUED);
        project.setProgress(0);
        project.setErrorMessage(null);
        project = projectRepository.save(project);

        return ProjectResponse.from(project);
    }

    private Project getProjectOrThrow(UUID projectId, String anonId) {
        return projectRepository
                .findByIdAndAnonId(projectId, anonId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Project not found."));
    }

    /** Anonymous users have no account, so cost control is IP + cookie based. */
    private void checkDailyLimit(String anonId, String clientIp) {
        Instant since = Instant.now().minus(Duration.ofHours(24));
        long count = projectRepository.countRecentByAnonIdOrClientIp(since, anonId, clientIp);
        if (count >= appProperties.getFreeDailyProjectLimit()) {
            throw new ApiException(
                    HttpStatus.TOO_MANY_REQUESTS, "Daily free video limit reached. Please try again tomorrow.");
        }
    }
}
