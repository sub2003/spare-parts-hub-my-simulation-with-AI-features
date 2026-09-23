package com.sliit.sparepartshub.reporting.repository;

import com.sliit.sparepartshub.entity.RestockOffer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PortalRestockOfferRepository extends JpaRepository<RestockOffer, Integer> {

    @EntityGraph(attributePaths = {"product", "supplier"})
    List<RestockOffer> findBySupplier_SupplierIdOrderBySubmittedAtDesc(Integer supplierId);

    @EntityGraph(attributePaths = {"product", "supplier"})
    Optional<RestockOffer> findByOfferIdAndSupplier_SupplierId(Integer offerId, Integer supplierId);
}
