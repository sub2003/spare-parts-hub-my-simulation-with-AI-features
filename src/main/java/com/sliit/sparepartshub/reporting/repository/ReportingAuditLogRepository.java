package com.sliit.sparepartshub.reporting.repository;

import com.sliit.sparepartshub.entity.AuditLog;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReportingAuditLogRepository extends JpaRepository<AuditLog, Integer> {

    @EntityGraph(attributePaths = "user")
    List<AuditLog> findTop500ByOrderByLoggedAtDesc();

    @EntityGraph(attributePaths = "user")
    List<AuditLog> findTop100ByOrderByLoggedAtDesc();

    @EntityGraph(attributePaths = "user")
    Optional<AuditLog> findByLogId(Integer logId);

    @EntityGraph(attributePaths = "user")
    List<AuditLog> findByLoggedAtGreaterThanEqualAndLoggedAtLessThanOrderByLoggedAtAsc(
            LocalDateTime from, LocalDateTime to);
}
