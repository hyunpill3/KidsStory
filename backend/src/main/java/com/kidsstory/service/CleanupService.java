package com.kidsstory.service;

import com.kidsstory.entity.Project;
import com.kidsstory.repository.ProjectRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deletes projects (and their stored photos/video) past their TTL. Replaces
 * the Celery-beat-scheduled worker.tasks.cleanup.purge_expired_projects
 * task. The MVP is anonymous with no library, so expired free-tier videos
 * are simply removed rather than archived.
 */
@Service
@RequiredArgsConstructor
public class CleanupService {

    private static final Logger log = LoggerFactory.getLogger(CleanupService.class);

    private final ProjectRepository projectRepository;
    private final StorageService storageService;

    @Transactional
    public int purgeExpiredProjects() {
        List<Project> expired = projectRepository.findByExpiresAtNotNullAndExpiresAtBefore(Instant.now());

        for (Project project : expired) {
            storageService.deleteProjectAssets(project.getId());
            projectRepository.delete(project);
        }

        if (!expired.isEmpty()) {
            log.info("Purged {} expired project(s)", expired.size());
        }
        return expired.size();
    }
}
