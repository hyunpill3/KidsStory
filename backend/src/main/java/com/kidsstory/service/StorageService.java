package com.kidsstory.service;

import java.nio.file.Path;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

/** Dual-backend storage: local disk for dev, R2 (S3-compatible) for production. */
public interface StorageService {

    /** Saves an uploaded photo + a generated thumbnail. Returns {url, thumbnailUrl}. */
    ImageUrls saveImage(UUID projectId, MultipartFile upload);

    /** Uploads the composed final MP4. Returns its public URL. */
    String saveVideo(UUID projectId, Path filePath);

    /** Deletes everything stored for a project (used by the expiry cleanup task). */
    void deleteProjectAssets(UUID projectId);

    /**
     * Reads a stored photo's bytes so it can be sent to the video-gen API.
     * Local storage: reads straight off disk. R2: the URL is already public,
     * fetched over HTTP.
     */
    byte[] loadImageBytes(String imageUrl);

    record ImageUrls(String url, String thumbnailUrl) {
    }
}
