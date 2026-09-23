package com.sliit.sparepartshub.reporting.repository;

import com.sliit.sparepartshub.entity.RmaClaim;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface ReportingRmaRepository extends JpaRepository<RmaClaim, Integer> {
    long countByResolution(RmaClaim.Resolution resolution);
    long countByClaimStatusIn(Collection<RmaClaim.ClaimStatus> statuses);
}
