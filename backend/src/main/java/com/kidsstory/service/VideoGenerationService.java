package com.kidsstory.service;

import com.kidsstory.config.AppProperties;
import com.kidsstory.entity.AgeGroup;
import com.kidsstory.entity.Language;
import com.kidsstory.entity.Project;
import com.kidsstory.entity.ProjectStatus;
import com.kidsstory.entity.Scene;
import com.kidsstory.entity.Video;
import com.kidsstory.entity.VideoStatus;
import com.kidsstory.entity.VisualStyle;
import com.kidsstory.entity.VoiceType;
import com.kidsstory.repository.ProjectRepository;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Runs the full AI generation pipeline for a single project. Replaces the
 * Python worker's Celery task (worker/worker/tasks/pipeline.py) - same
 * stage sequence, same status/progress values the frontend already polls
 * for, just running in-process on a dedicated executor instead of a
 * separate broker-backed worker.
 *
 * <p>Each stage update runs in its own short transaction (via
 * TransactionTemplate) rather than one transaction for the whole method:
 * this mirrors the Python task's incremental db.commit() calls, so a
 * polling GET /status/ sees live progress, and it avoids holding a
 * database connection open for the several minutes this method can run.
 */
@Service
public class VideoGenerationService {

    private static final Logger log = LoggerFactory.getLogger(VideoGenerationService.class);

    private static final Map<ProjectStatus, Integer> PROGRESS_BY_STATUS = Map.of(
            ProjectStatus.ANALYZING_PHOTOS, 15,
            ProjectStatus.GENERATING_STORY, 30,
            ProjectStatus.GENERATING_SCENES, 45,
            ProjectStatus.GENERATING_NARRATION, 65,
            ProjectStatus.COMPOSING_AUDIO, 80,
            ProjectStatus.RENDERING_VIDEO, 95,
            ProjectStatus.COMPLETED, 100);

    private final ProjectRepository projectRepository;
    private final StorageService storageService;
    private final AppProperties appProperties;
    private final PhotoAnalysisService photoAnalysisService;
    private final StoryGenerationService storyGenerationService;
    private final SceneSplittingService sceneSplittingService;
    private final SceneRenderingService sceneRenderingService;
    private final NarrationService narrationService;
    private final AudioMixingService audioMixingService;
    private final VideoCompositionService videoCompositionService;
    private final NotificationService notificationService;
    private final TransactionTemplate transactionTemplate;

    public VideoGenerationService(
            ProjectRepository projectRepository,
            StorageService storageService,
            AppProperties appProperties,
            PhotoAnalysisService photoAnalysisService,
            StoryGenerationService storyGenerationService,
            SceneSplittingService sceneSplittingService,
            SceneRenderingService sceneRenderingService,
            NarrationService narrationService,
            AudioMixingService audioMixingService,
            VideoCompositionService videoCompositionService,
            NotificationService notificationService,
            PlatformTransactionManager transactionManager) {
        this.projectRepository = projectRepository;
        this.storageService = storageService;
        this.appProperties = appProperties;
        this.photoAnalysisService = photoAnalysisService;
        this.storyGenerationService = storyGenerationService;
        this.sceneSplittingService = sceneSplittingService;
        this.sceneRenderingService = sceneRenderingService;
        this.narrationService = narrationService;
        this.audioMixingService = audioMixingService;
        this.videoCompositionService = videoCompositionService;
        this.notificationService = notificationService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Async("videoGenerationExecutor")
    public void processProjectAsync(UUID projectId) {
        List<SceneDraft> scenes = null;
        try {
            ProjectSnapshot snapshot = loadSnapshot(projectId);
            String sourceImageUrl = snapshot.imageUrls().isEmpty() ? null : snapshot.imageUrls().get(0);

            updateStatus(projectId, ProjectStatus.ANALYZING_PHOTOS);
            String photoInsight = photoAnalysisService.analyzePhotos(snapshot.imageUrls());

            updateStatus(projectId, ProjectStatus.GENERATING_STORY);
            String story = storyGenerationService.generateStory(
                    snapshot.storyPrompt(), photoInsight, snapshot.ageGroup());
            updateGeneratedStory(projectId, story);

            updateStatus(projectId, ProjectStatus.GENERATING_SCENES);
            scenes = sceneSplittingService.splitIntoScenes(story, snapshot.videoLength());
            scenes = sceneRenderingService.renderScenes(scenes, snapshot.style().getValue(), sourceImageUrl);

            updateStatus(projectId, ProjectStatus.GENERATING_NARRATION);
            scenes = narrationService.generateNarration(scenes, snapshot.voice(), snapshot.language());

            updateStatus(projectId, ProjectStatus.COMPOSING_AUDIO);
            scenes = audioMixingService.addMusicAndSfx(scenes);
            persistScenes(projectId, scenes, sourceImageUrl);

            updateStatus(projectId, ProjectStatus.RENDERING_VIDEO);
            var localVideoPath = videoCompositionService.composeFinalVideo(
                    scenes, projectId.toString(), appProperties.getWatermarkText());
            String videoUrl = storageService.saveVideo(projectId, localVideoPath);
            Files.deleteIfExists(localVideoPath);

            // Only clean up the raw per-scene clips on success. They are the
            // paid-for Replicate output - if a later, free, local step (e.g.
            // ffmpeg) fails, leave them on disk rather than silently discard
            // something that already cost money to generate.
            for (SceneDraft scene : scenes) {
                deleteQuietly(scene.getLocalVideoPath());
            }

            completeProject(projectId, videoUrl, snapshot.videoLength());
            notificationService.notifyUser(projectId.toString(), true);
        } catch (Exception ex) {
            log.error("Video generation failed for project {}", projectId, ex);
            if (scenes != null) {
                for (SceneDraft scene : scenes) {
                    if (scene.getLocalVideoPath() != null) {
                        log.warn(
                                "Preserving already-generated (paid-for) scene clip after failure: {}",
                                scene.getLocalVideoPath());
                    }
                }
            }
            markFailed(projectId, ex.getMessage());
            notificationService.notifyUser(projectId.toString(), false);
        }
    }

    private void deleteQuietly(java.nio.file.Path path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (Exception ignored) {
            // best-effort temp file cleanup
        }
    }

    private ProjectSnapshot loadSnapshot(UUID projectId) {
        return transactionTemplate.execute(status -> {
            Project project = requireProject(projectId);
            List<String> imageUrls = project.getImages().stream().map(image -> image.getUrl()).toList();
            return new ProjectSnapshot(
                    project.getStoryPrompt(),
                    project.getAgeGroup(),
                    project.getVideoLength(),
                    project.getStyle(),
                    project.getVoice(),
                    project.getLanguage(),
                    imageUrls);
        });
    }

    private void updateStatus(UUID projectId, ProjectStatus newStatus) {
        transactionTemplate.executeWithoutResult(status -> {
            Project project = requireProject(projectId);
            project.setStatus(newStatus);
            project.setProgress(PROGRESS_BY_STATUS.getOrDefault(newStatus, project.getProgress()));
            projectRepository.save(project);
        });
    }

    private void updateGeneratedStory(UUID projectId, String story) {
        transactionTemplate.executeWithoutResult(status -> {
            Project project = requireProject(projectId);
            project.setGeneratedStory(story);
            projectRepository.save(project);
        });
    }

    private void persistScenes(UUID projectId, List<SceneDraft> scenes, String sourceImageUrl) {
        transactionTemplate.executeWithoutResult(status -> {
            Project project = requireProject(projectId);
            for (SceneDraft draft : scenes) {
                Scene scene = new Scene();
                scene.setProject(project);
                scene.setDisplayOrder(draft.getIndex());
                scene.setNarration(draft.getNarration());
                scene.setVisualDescription(draft.getVisualDescription());
                scene.setImageUrl(sourceImageUrl);
                // Intermediate scene clips are local temp files, not storage
                // URLs; only the final composed video is persisted.
                scene.setVideoUrl(null);
                scene.setNarrationAudioUrl(draft.getNarrationAudioPath());
                scene.setMixedAudioUrl(draft.getMixedAudioPath());
                project.getScenes().add(scene);
            }
            projectRepository.save(project);
        });
    }

    private void completeProject(UUID projectId, String videoUrl, int durationSeconds) {
        transactionTemplate.executeWithoutResult(status -> {
            Project project = requireProject(projectId);

            Video video = new Video();
            video.setProject(project);
            video.setStatus(VideoStatus.COMPLETED);
            video.setUrl(videoUrl);
            video.setDurationSeconds(durationSeconds);
            project.getVideos().add(video);

            project.setStatus(ProjectStatus.COMPLETED);
            project.setProgress(PROGRESS_BY_STATUS.get(ProjectStatus.COMPLETED));
            projectRepository.save(project);
        });
    }

    private void markFailed(UUID projectId, String message) {
        transactionTemplate.executeWithoutResult(status -> {
            Project project = projectRepository.findById(projectId).orElse(null);
            if (project == null) return;

            project.setStatus(ProjectStatus.FAILED);
            project.setErrorMessage(message);

            Video video = new Video();
            video.setProject(project);
            video.setStatus(VideoStatus.FAILED);
            video.setErrorMessage(message);
            project.getVideos().add(video);

            projectRepository.save(project);
        });
    }

    private Project requireProject(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalStateException("Project not found: " + projectId));
    }

    private record ProjectSnapshot(
            String storyPrompt,
            AgeGroup ageGroup,
            int videoLength,
            VisualStyle style,
            VoiceType voice,
            Language language,
            List<String> imageUrls) {
    }
}
