package com.kidsstory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kidsstory.entity.Project;
import com.kidsstory.repository.ProjectRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CleanupServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private StorageService storageService;

    private CleanupService cleanupService;

    @BeforeEach
    void setUp() {
        cleanupService = new CleanupService(projectRepository, storageService);
    }

    @Test
    void deletesStorageAndDbRowForEveryExpiredProject() {
        Project first = projectWithId();
        Project second = projectWithId();
        when(projectRepository.findByExpiresAtNotNullAndExpiresAtBefore(any())).thenReturn(List.of(first, second));

        int purged = cleanupService.purgeExpiredProjects();

        assertThat(purged).isEqualTo(2);
        verify(storageService).deleteProjectAssets(first.getId());
        verify(storageService).deleteProjectAssets(second.getId());
        verify(projectRepository).delete(first);
        verify(projectRepository).delete(second);
    }

    @Test
    void doesNothingWhenNoProjectsHaveExpired() {
        when(projectRepository.findByExpiresAtNotNullAndExpiresAtBefore(any())).thenReturn(List.of());

        int purged = cleanupService.purgeExpiredProjects();

        assertThat(purged).isZero();
        verify(storageService, never()).deleteProjectAssets(any());
        verify(projectRepository, never()).delete(any());
    }

    private Project projectWithId() {
        Project project = new Project();
        project.setId(UUID.randomUUID());
        return project;
    }
}
