package com.kidsstory.repository;

import com.kidsstory.entity.Project;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    Optional<Project> findByIdAndAnonId(UUID id, String anonId);

    @Query("""
            select count(p) from Project p
            where p.createdAt >= :since
            and (p.anonId = :anonId or p.clientIp = :clientIp)
            """)
    long countRecentByAnonIdOrClientIp(
            @Param("since") Instant since, @Param("anonId") String anonId, @Param("clientIp") String clientIp);

    List<Project> findByExpiresAtNotNullAndExpiresAtBefore(Instant cutoff);
}
