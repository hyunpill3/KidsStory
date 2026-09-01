package com.kidsstory.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kidsstory.config.AppProperties;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Step 4: Per-scene image-to-video rendering.
 *
 * <p>Calls Replicate's hosted Kling v1.6 model (photo + a motion prompt -&gt;
 * a short video clip) for each scene, using the project's photo as the
 * starting frame. There is no official Replicate Java SDK, so this talks to
 * the documented HTTP API directly and polls for completion (the Python
 * SDK's client.run() did this internally; here it's explicit).
 */
@Service
public class SceneRenderingService {

    private static final String REPLICATE_API_BASE = "https://api.replicate.com/v1";
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(3);
    private static final Duration POLL_TIMEOUT = Duration.ofMinutes(10);
    private static final int IMAGE_MAX_DIMENSION = 1024;

    private static final Map<String, String> STYLE_PROMPTS = Map.of(
            "3d_cute", "soft, cuddly 3D animated storybook style",
            "storybook", "warm, hand-drawn children's storybook illustration style",
            "watercolor", "gentle watercolor illustration style",
            "cartoon", "playful, colorful cartoon style");

    private static final String MOTION_TEMPLATE =
            "Create a subtle cinematic animation from this children's story illustration, "
                    + "in a %s. Add gentle camera movement, natural motion, and a warm, "
                    + "magical storybook feeling. %s";

    private final AppProperties appProperties;
    private final StorageService storageService;
    private final RestClient restClient = RestClient.create();

    public SceneRenderingService(AppProperties appProperties, StorageService storageService) {
        this.appProperties = appProperties;
        this.storageService = storageService;
    }

    public List<SceneDraft> renderScenes(List<SceneDraft> scenes, String style, String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalStateException("At least one project photo is required to render a scene.");
        }

        String imageDataUri = toDownscaledDataUri(storageService.loadImageBytes(imageUrl));
        String stylePrompt = STYLE_PROMPTS.getOrDefault(style, "storybook illustration style");

        for (SceneDraft scene : scenes) {
            String prompt = MOTION_TEMPLATE.formatted(stylePrompt, scene.getVisualDescription());
            byte[] videoBytes = generateClip(prompt, imageDataUri);

            Path localPath = tempClipPath(scene.getIndex());
            try {
                Files.write(localPath, videoBytes);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }

            scene.setStyle(style);
            scene.setLocalVideoPath(localPath);
        }

        return scenes;
    }

    private byte[] generateClip(String prompt, String imageDataUri) {
        Map<String, Object> input = Map.of(
                "prompt", prompt,
                "start_image", imageDataUri,
                // Matches SECONDS_PER_SCENE / the free tier's advertised 10s video length.
                "duration", 10,
                "negative_prompt", "blurry, distorted, extra limbs, text overlays");

        Prediction created = restClient
                .post()
                .uri(REPLICATE_API_BASE + "/models/" + appProperties.getVideoGenModel() + "/predictions")
                .headers(headers -> headers.setBearerAuth(appProperties.getVideoGenApiKey()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("input", input))
                .retrieve()
                .body(Prediction.class);

        Prediction finished = pollUntilDone(created);

        if (!"succeeded".equals(finished.status()) || finished.output() == null) {
            String error = finished.error() != null ? finished.error() : "unknown error";
            throw new IllegalStateException("Video generation failed: " + error);
        }

        // Output files require the same bearer token to fetch (per Replicate's docs).
        return restClient
                .get()
                .uri(finished.output())
                .headers(headers -> headers.setBearerAuth(appProperties.getVideoGenApiKey()))
                .retrieve()
                .body(byte[].class);
    }

    private Prediction pollUntilDone(Prediction prediction) {
        Instant deadline = Instant.now().plus(POLL_TIMEOUT);
        Prediction current = prediction;

        while (isPending(current.status())) {
            if (Instant.now().isAfter(deadline)) {
                throw new IllegalStateException("Timed out waiting for video generation to complete.");
            }
            sleep(POLL_INTERVAL);
            current = restClient
                    .get()
                    .uri(REPLICATE_API_BASE + "/predictions/" + current.id())
                    .headers(headers -> headers.setBearerAuth(appProperties.getVideoGenApiKey()))
                    .retrieve()
                    .body(Prediction.class);
        }

        return current;
    }

    private boolean isPending(String status) {
        return "starting".equals(status) || "processing".equals(status);
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for video generation.", ex);
        }
    }

    private String toDownscaledDataUri(byte[] originalBytes) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Thumbnails.of(new ByteArrayInputStream(originalBytes))
                    .size(IMAGE_MAX_DIMENSION, IMAGE_MAX_DIMENSION)
                    .outputFormat("jpg")
                    .outputQuality(0.85)
                    .toOutputStream(out);
            return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private Path tempClipPath(int sceneIndex) {
        return Path.of(System.getProperty("java.io.tmpdir"), "kidsstory_scene_" + sceneIndex + ".mp4");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Prediction(String id, String status, String output, String error) {
    }
}
