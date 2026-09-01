package com.kidsstory.repository;

import com.kidsstory.entity.ProjectImage;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectImageRepository extends JpaRepository<ProjectImage, UUID> {
}
