package com.kidsstory.service;

import com.kidsstory.config.AppProperties;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Cloudflare R2 (S3-compatible) storage. Photos and videos never touch
 * Postgres. Key layout: projects/{projectId}/{fileId}.jpg and
 * projects/{projectId}/final.mp4 - a shared prefix per project makes it
 * easy to purge everything once a project expires.
 *
 * <p>Not a {@code @Component} - {@link StorageConfig} picks exactly one
 * {@link StorageService} implementation based on {@code app.storage-backend}.
 */
public class R2StorageService implements StorageService {

    private static final int THUMBNAIL_SIZE = 400;

    private final AppProperties appProperties;
    private final S3Client s3Client;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public R2StorageService(AppProperties appProperties) {
        this.appProperties = appProperties;
        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(appProperties.getR2EndpointUrl()))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                                appProperties.getR2AccessKeyId(), appProperties.getR2SecretAccessKey())))
                .build();
    }

    @Override
    public ImageUrls saveImage(UUID projectId, MultipartFile upload) {
        String extension = extensionOf(upload.getOriginalFilename());
        String fileId = UUID.randomUUID().toString().replace("-", "");
        String baseKey = "projects/" + projectId + "/" + fileId;
        String originalKey = baseKey + extension;
        String thumbnailKey = baseKey + "_thumb.jpg";

        byte[] originalBytes;
        try (InputStream input = upload.getInputStream()) {
            originalBytes = input.readAllBytes();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }

        String bucket = appProperties.getR2BucketName();
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(originalKey)
                        .contentType(upload.getContentType() != null ? upload.getContentType() : "image/jpeg")
                        .build(),
                RequestBody.fromBytes(originalBytes));

        s3Client.putObject(
                PutObjectRequest.builder().bucket(bucket).key(thumbnailKey).contentType("image/jpeg").build(),
                RequestBody.fromBytes(thumbnailBytes(originalBytes)));

        return new ImageUrls(toUrl(originalKey), toUrl(thumbnailKey));
    }

    @Override
    public String saveVideo(UUID projectId, Path filePath) {
        String key = "projects/" + projectId + "/final.mp4";
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(appProperties.getR2BucketName())
                        .key(key)
                        .contentType("video/mp4")
                        .build(),
                RequestBody.fromFile(filePath));
        return toUrl(key);
    }

    @Override
    public void deleteProjectAssets(UUID projectId) {
        String prefix = "projects/" + projectId + "/";
        String bucket = appProperties.getR2BucketName();

        String continuationToken = null;
        do {
            ListObjectsV2Request.Builder requestBuilder =
                    ListObjectsV2Request.builder().bucket(bucket).prefix(prefix);
            if (continuationToken != null) {
                requestBuilder.continuationToken(continuationToken);
            }
            ListObjectsV2Response response = s3Client.listObjectsV2(requestBuilder.build());

            List<ObjectIdentifier> keys = new ArrayList<>();
            response.contents().forEach(obj -> keys.add(ObjectIdentifier.builder().key(obj.key()).build()));

            if (!keys.isEmpty()) {
                s3Client.deleteObjects(DeleteObjectsRequest.builder()
                        .bucket(bucket)
                        .delete(Delete.builder().objects(keys).build())
                        .build());
            }

            continuationToken = response.isTruncated() ? response.nextContinuationToken() : null;
        } while (continuationToken != null);
    }

    @Override
    public byte[] loadImageBytes(String imageUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(imageUrl)).GET().build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("Failed to fetch image, HTTP " + response.statusCode());
            }
            return response.body();
        } catch (IOException | InterruptedException ex) {
            throw new RuntimeException("Failed to fetch image from " + imageUrl, ex);
        }
    }

    private byte[] thumbnailBytes(byte[] originalBytes) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Thumbnails.of(new java.io.ByteArrayInputStream(originalBytes))
                    .size(THUMBNAIL_SIZE, THUMBNAIL_SIZE)
                    .outputFormat("jpg")
                    .outputQuality(0.85)
                    .toOutputStream(out);
            return out.toByteArray();
        } catch (IOException ex) {
            return originalBytes;
        }
    }

    private String toUrl(String key) {
        return appProperties.getR2PublicUrl().replaceAll("/$", "") + "/" + key;
    }

    private String extensionOf(String filename) {
        if (filename == null) return ".jpg";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : ".jpg";
    }
}
