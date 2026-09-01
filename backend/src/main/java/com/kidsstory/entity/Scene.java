package com.kidsstory.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A single scene within a project's story (visual + narration + audio). */
@Entity
@Table(name = "scenes")
@Getter
@Setter
@NoArgsConstructor
public class Scene {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    // Named displayOrder (column display_order) since ORDER is a reserved SQL keyword.
    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(nullable = false, columnDefinition = "text")
    private String narration;

    @Column(name = "visual_description", columnDefinition = "text")
    private String visualDescription;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "video_url", length = 500)
    private String videoUrl;

    @Column(name = "narration_audio_url", length = 500)
    private String narrationAudioUrl;

    @Column(name = "mixed_audio_url", length = 500)
    private String mixedAudioUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
