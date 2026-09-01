package com.kidsstory.controller;

import com.kidsstory.dto.ProjectCreateRequest;
import com.kidsstory.dto.ProjectResponse;
import com.kidsstory.dto.ProjectStatusResponse;
import com.kidsstory.exception.ApiException;
import com.kidsstory.service.AnonIdentity;
import com.kidsstory.service.CaptchaService;
import com.kidsstory.service.ProjectService;
import com.kidsstory.service.VideoGenerationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final CaptchaService captchaService;
    private final VideoGenerationService videoGenerationService;

    @PostMapping("/")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse createProject(@Valid @RequestBody ProjectCreateRequest payload, AnonIdentity identity) {
        if (!captchaService.verifyCaptcha(payload.captchaToken(), identity.clientIp())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Captcha verification failed.");
        }

        return projectService.createProject(identity, payload);
    }

    @PostMapping("/{projectId}/upload/")
    public ProjectResponse uploadImages(
            @PathVariable UUID projectId,
            @RequestParam("files") List<MultipartFile> files,
            AnonIdentity identity) {
        return projectService.addImages(projectId, identity.anonId(), files);
    }

    @PostMapping("/{projectId}/generate/")
    public ProjectResponse generateVideo(@PathVariable UUID projectId, AnonIdentity identity) {
        ProjectResponse response = projectService.markQueued(projectId, identity.anonId());
        // Dispatched only after markQueued's transaction has committed (see
        // its Javadoc) so the async pipeline never races the QUEUED write.
        videoGenerationService.processProjectAsync(projectId);
        return response;
    }

    @GetMapping("/{projectId}/status/")
    public ProjectStatusResponse getStatus(@PathVariable UUID projectId, AnonIdentity identity) {
        return projectService.getStatus(projectId, identity.anonId());
    }
}
