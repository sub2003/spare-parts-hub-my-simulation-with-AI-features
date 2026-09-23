package com.sliit.sparepartshub.supplier.repository;

import com.sliit.sparepartshub.entity.PartnershipRequest;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PartnershipRequestRepository extends JpaRepository<PartnershipRequest, Integer> {
    @EntityGraph(attributePaths = {"supplier", "reviewedBy"})
    List<PartnershipRequest> findAllByOrderBySubmittedAtDesc();

    @Override
    @EntityGraph(attributePaths = {"supplier", "reviewedBy"})
    Optional<PartnershipRequest> findById(Integer id);

    boolean existsBySupplier_SupplierId(Integer supplierId);
}
