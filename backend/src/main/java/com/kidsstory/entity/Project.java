package com.kidsstory.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
public class Project {

    @Id
    @GeneratedValue
    private UUID id;

    // Nullable: the anonymous MVP has no accounts. userId is reserved for a
    // future signed-in flow; anonId/clientIp identify anonymous owners.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "anon_id", length = 64)
    private String anonId;

    @Column(name = "client_ip", length = 64)
    private String clientIp;

    @Column(nullable = false, length = 200)
    private String title = "Untitled Story";

    @Column(name = "story_prompt", columnDefinition = "text")
    private String storyPrompt;

    @Column(name = "generated_story", columnDefinition = "text")
    private String generatedStory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Plan plan = Plan.FREE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProjectStatus status = ProjectStatus.DRAFT;

    @Column(nullable = false)
    private int progress = 0;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "age_group", nullable = false, length = 10)
    private AgeGroup ageGroup = AgeGroup.AGE_3_5;

    @Column(name = "video_length", nullable = false)
    private int videoLength = 30;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VisualStyle style = VisualStyle.THREE_D_CUTE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VoiceType voice = VoiceType.CALM_BEDTIME;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private Language language = Language.KO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Free/anonymous videos are temporary; a scheduled cleanup task purges
    // rows (and their storage objects) once past this timestamp.
    @Column(name = "expires_at")
    private Instant expiresAt;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<ProjectImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<Scene> scenes = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Video> videos = new ArrayList<>();

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    /** Videos ordered newest-first, matching the Python relationship's order_by. */
    public List<Video> getVideosNewestFirst() {
        return videos.stream()
                .sorted(Comparator.comparing(Video::getCreatedAt).reversed())
                .toList();
    }
}
