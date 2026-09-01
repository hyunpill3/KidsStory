package com.kidsstory.repository;

import com.kidsstory.entity.Video;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoRepository extends JpaRepository<Video, UUID> {
}
