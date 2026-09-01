package com.kidsstory.config;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Mirrors the union of the Python backend's and worker's Settings classes. */
@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String environment = "local";
    private String secretKey = "change-this-to-a-random-secret-key";
    private int accessTokenExpireMinutes = 60;

    private List<String> allowedOrigins = List.of("http://localhost:3000");

    private String storageBackend = "local";
    private String mediaRoot = "./media";
    private String mediaUrl = "http://localhost:8000/media";

    private String r2AccountId = "";
    private String r2AccessKeyId = "";
    private String r2SecretAccessKey = "";
    private String r2BucketName = "kidsstory";
    private String r2EndpointUrl = "";
    private String r2PublicUrl = "";

    private String anonCookieName = "ks_anon_id";
    private int anonCookieMaxAgeDays = 30;

    private int maxPhotosFree = 1;
    private int maxVideoLengthFree = 10;
    private int freeDailyProjectLimit = 1;
    private String watermarkText = "KidsStory";
    // Blank = auto-detect per OS (Windows: bundled Arial; Linux: fonts-dejavu-core).
    // See VideoCompositionService - ffmpeg's drawtext filter needs an explicit
    // fontfile to avoid a fontconfig-related crash on Windows builds.
    private String watermarkFontPath = "";
    private long maxUploadBytes = 8L * 1024 * 1024;

    private int projectTtlHours = 24;
    private int cleanupIntervalMinutes = 60;

    private String turnstileSecretKey = "";

    private String videoGenApiKey = "";
    private String videoGenModel = "kwaivgi/kling-v1.6-standard";
}
