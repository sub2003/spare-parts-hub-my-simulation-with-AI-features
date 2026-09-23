package com.sliit.sparepartshub.reporting.repository;

import com.sliit.sparepartshub.entity.PartnershipRequest;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PortalPartnershipRepository extends JpaRepository<PartnershipRequest, Integer> {
    @EntityGraph(attributePaths = {"supplier", "reviewedBy"})
    List<PartnershipRequest> findBySupplier_SupplierIdOrderBySubmittedAtDesc(Integer supplierId);

    @EntityGraph(attributePaths = {"supplier", "reviewedBy"})
    Optional<PartnershipRequest> findByRequestIdAndSupplier_SupplierId(Integer requestId, Integer supplierId);

    boolean existsBySupplier_SupplierIdAndStatus(Integer supplierId, PartnershipRequest.Status status);
}
