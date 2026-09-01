package com.kidsstory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kidsstory.config.AppProperties;
import com.kidsstory.dto.ProjectCreateRequest;
import com.kidsstory.dto.ProjectOptions;
import com.kidsstory.dto.ProjectResponse;
import com.kidsstory.entity.AgeGroup;
import com.kidsstory.entity.Language;
import com.kidsstory.entity.Plan;
import com.kidsstory.entity.Project;
import com.kidsstory.entity.ProjectStatus;
import com.kidsstory.entity.VisualStyle;
import com.kidsstory.entity.VoiceType;
import com.kidsstory.exception.ApiException;
import com.kidsstory.repository.ProjectRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private StorageService storageService;

    private AppProperties appProperties;
    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.setMaxVideoLengthFree(10);
        appProperties.setMaxPhotosFree(1);
        appProperties.setMaxUploadBytes(8L * 1024 * 1024);
        appProperties.setFreeDailyProjectLimit(1);
        appProperties.setProjectTtlHours(24);

        projectService = new ProjectService(projectRepository, storageService, appProperties);

        // save() round-trips the same (mutated) entity back, like a real JPA repository would.
        // lenient(): several tests throw before ever reaching save(), which strict stubbing
        // would otherwise flag as an unused stub.
        lenient()
                .when(projectRepository.save(any(Project.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private ProjectCreateRequest createRequest(String storyPrompt, int videoLength) {
        return new ProjectCreateRequest(
                storyPrompt,
                new ProjectOptions(AgeGroup.AGE_3_5, videoLength, VisualStyle.STORYBOOK, VoiceType.MALE, Language.KO),
                null);
    }

    @Test
    void createProject_capsVideoLengthToFreeTierMax() {
        when(projectRepository.countRecentByAnonIdOrClientIp(any(), anyString(), anyString())).thenReturn(0L);

        ProjectResponse response = projectService.createProject(
                new AnonIdentity("anon-1", "127.0.0.1"), createRequest("A story", 999));

        assertThat(response.options().videoLength()).isEqualTo(10);
        assertThat(response.plan()).isEqualTo(Plan.FREE);
        assertThat(response.status()).isEqualTo(ProjectStatus.DRAFT);
    }

    @Test
    void createProject_usesUntitledStoryWhenPromptBlank() {
        when(projectRepository.countRecentByAnonIdOrClientIp(any(), anyString(), anyString())).thenReturn(0L);

        ProjectResponse response =
                projectService.createProject(new AnonIdentity("anon-1", "127.0.0.1"), createRequest("  ", 10));

        assertThat(response.title()).isEqualTo("Untitled Story");
    }

    @Test
    void createProject_truncatesLongTitleTo60Chars() {
        when(projectRepository.countRecentByAnonIdOrClientIp(any(), anyString(), anyString())).thenReturn(0L);
        String longPrompt = "x".repeat(200);

        ProjectResponse response =
                projectService.createProject(new AnonIdentity("anon-1", "127.0.0.1"), createRequest(longPrompt, 10));

        assertThat(response.title()).hasSize(60);
    }

    @Test
    void createProject_rejectsWhenDailyLimitReached() {
        when(projectRepository.countRecentByAnonIdOrClientIp(any(), anyString(), anyString())).thenReturn(1L);

        assertThatThrownBy(() ->
                        projectService.createProject(new AnonIdentity("anon-1", "127.0.0.1"), createRequest("hi", 10)))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        verify(projectRepository, never()).save(any());
    }

    @Test
    void getStatus_throwsNotFoundForUnknownProject() {
        UUID projectId = UUID.randomUUID();
        when(projectRepository.findByIdAndAnonId(projectId, "anon-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getStatus(projectId, "anon-1"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void addImages_rejectsEmptyFileList() {
        UUID projectId = UUID.randomUUID();
        Project project = new Project();
        when(projectRepository.findByIdAndAnonId(projectId, "anon-1")).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.addImages(projectId, "anon-1", List.of()))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void addImages_rejectsMoreThanFreeTierPhotoLimit() {
        UUID projectId = UUID.randomUUID();
        Project project = new Project();
        when(projectRepository.findByIdAndAnonId(projectId, "anon-1")).thenReturn(Optional.of(project));

        MockMultipartFile file1 = new MockMultipartFile("files", "a.jpg", "image/jpeg", new byte[]{1});
        MockMultipartFile file2 = new MockMultipartFile("files", "b.jpg", "image/jpeg", new byte[]{1});

        assertThatThrownBy(() -> projectService.addImages(projectId, "anon-1", List.of(file1, file2)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("1 photo(s)");
    }

    @Test
    void addImages_rejectsOversizedFile() {
        UUID projectId = UUID.randomUUID();
        Project project = new Project();
        when(projectRepository.findByIdAndAnonId(projectId, "anon-1")).thenReturn(Optional.of(project));

        byte[] tooLarge = new byte[(int) appProperties.getMaxUploadBytes() + 1];
        MockMultipartFile file = new MockMultipartFile("files", "big.jpg", "image/jpeg", tooLarge);

        assertThatThrownBy(() -> projectService.addImages(projectId, "anon-1", List.of(file)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("smaller than");

        verify(storageService, never()).saveImage(any(), any());
    }

    @Test
    void addImages_savesUploadedPhotoAndReturnsItsUrls() {
        UUID projectId = UUID.randomUUID();
        Project project = new Project();
        when(projectRepository.findByIdAndAnonId(projectId, "anon-1")).thenReturn(Optional.of(project));
        when(storageService.saveImage(any(), any()))
                .thenReturn(new StorageService.ImageUrls("http://x/photo.jpg", "http://x/thumb.jpg"));

        MockMultipartFile file = new MockMultipartFile("files", "a.jpg", "image/jpeg", new byte[]{1, 2, 3});

        ProjectResponse response = projectService.addImages(projectId, "anon-1", List.of(file));

        assertThat(response.images()).hasSize(1);
        assertThat(response.images().get(0).url()).isEqualTo("http://x/photo.jpg");
        assertThat(response.images().get(0).order()).isZero();
    }

    @Test
    void markQueued_rejectsProjectWithNoPhotos() {
        UUID projectId = UUID.randomUUID();
        Project project = new Project();
        when(projectRepository.findByIdAndAnonId(projectId, "anon-1")).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.markQueued(projectId, "anon-1"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("upload a photo first");
    }

    @Test
    void markQueued_resetsProgressAndClearsPreviousError() {
        UUID projectId = UUID.randomUUID();
        Project project = new Project();
        project.setStatus(ProjectStatus.FAILED);
        project.setProgress(45);
        project.setErrorMessage("previous failure");
        project.getImages().add(new com.kidsstory.entity.ProjectImage());

        when(projectRepository.findByIdAndAnonId(projectId, "anon-1")).thenReturn(Optional.of(project));

        ProjectResponse response = projectService.markQueued(projectId, "anon-1");

        assertThat(response.status()).isEqualTo(ProjectStatus.QUEUED);
        assertThat(response.progress()).isZero();
        assertThat(project.getErrorMessage()).isNull();
    }
}
