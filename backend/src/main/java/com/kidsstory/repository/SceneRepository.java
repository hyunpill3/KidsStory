package com.kidsstory.repository;

import com.kidsstory.entity.Scene;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SceneRepository extends JpaRepository<Scene, UUID> {
}
