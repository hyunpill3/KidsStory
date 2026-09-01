package com.kidsstory.service;

import com.kidsstory.config.AppProperties;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.web.multipart.MultipartFile;

/**
 * Local-disk storage for development. Everything for a project lives under
 * one prefix so expired projects can be purged with a single delete:
 * media/projects/{projectId}/{fileId}.jpg, media/projects/{projectId}/final.mp4
 *
 * <p>Not a {@code @Component} - {@link StorageConfig} picks exactly one
 * {@link StorageService} implementation based on {@code app.storage-backend}.
 */
public class LocalStorageService implements StorageService {

    private static final int THUMBNAIL_SIZE = 400;

    private final AppProperties appProperties;

    public LocalStorageService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public ImageUrls saveImage(UUID projectId, MultipartFile upload) {
        Path directory = projectDir(projectId);

        String extension = extensionOf(upload.getOriginalFilename());
        String fileId = UUID.randomUUID().toString().replace("-", "");
        Path originalPath = directory.resolve(fileId + extension);
        Path thumbnailPath = directory.resolve(fileId + "_thumb.jpg");

        try {
            upload.transferTo(originalPath);
            try {
                Thumbnails.of(originalPath.toFile())
                        .size(THUMBNAIL_SIZE, THUMBNAIL_SIZE)
                        .outputFormat("jpg")
                        .outputQuality(0.85)
                        .toFile(thumbnailPath.toFile());
            } catch (IOException thumbnailFailure) {
                Files.copy(originalPath, thumbnailPath);
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }

        return new ImageUrls(toUrl(originalPath), toUrl(thumbnailPath));
    }

    @Override
    public String saveVideo(UUID projectId, Path filePath) {
        Path directory = projectDir(projectId);
        Path destination = directory.resolve("final.mp4");
        try {
            Files.copy(filePath, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        return toUrl(destination);
    }

    @Override
    public void deleteProjectAssets(UUID projectId) {
        Path directory = mediaRoot().resolve("projects").resolve(projectId.toString());
        try {
            if (Files.exists(directory)) {
                try (var walk = Files.walk(directory)) {
                    walk.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException ignored) {
                            // best-effort cleanup, matches the Python version's ignore_errors=True
                        }
                    });
                }
            }
        } catch (IOException ignored) {
            // best-effort cleanup
        }
    }

    @Override
    public byte[] loadImageBytes(String imageUrl) {
        String mediaUrl = appProperties.getMediaUrl().replaceAll("/$", "");
        if (!imageUrl.startsWith(mediaUrl)) {
            throw new IllegalArgumentException("Not a local media URL: " + imageUrl);
        }
        String relative = imageUrl.substring(mediaUrl.length()).replaceFirst("^/", "");
        try {
            return Files.readAllBytes(mediaRoot().resolve(relative));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private Path mediaRoot() {
        return Path.of(appProperties.getMediaRoot());
    }

    private Path projectDir(UUID projectId) {
        Path directory = mediaRoot().resolve("projects").resolve(projectId.toString());
        try {
            Files.createDirectories(directory);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        return directory;
    }

    private String toUrl(Path path) {
        Path relative = mediaRoot().toAbsolutePath().relativize(path.toAbsolutePath());
        String relativeUrl = relative.toString().replace('\\', '/');
        return appProperties.getMediaUrl().replaceAll("/$", "") + "/" + relativeUrl;
    }

    private String extensionOf(String filename) {
        if (filename == null) return ".jpg";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : ".jpg";
    }
}
