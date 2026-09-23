package com.sliit.sparepartshub.reporting.repository;

import com.sliit.sparepartshub.entity.AuditReview;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReportingAuditReviewRepository extends JpaRepository<AuditReview, Integer> {

    @EntityGraph(attributePaths = {"reviewedBy", "auditLog"})
    Optional<AuditReview> findByAuditLog_LogId(Integer logId);

    @EntityGraph(attributePaths = {"reviewedBy", "auditLog"})
    List<AuditReview> findByAuditLog_LogIdIn(Collection<Integer> logIds);
}
