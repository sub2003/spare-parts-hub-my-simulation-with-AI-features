package com.sliit.sparepartshub.supplier.repository;

import com.sliit.sparepartshub.entity.RestockOffer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RestockOfferRepository extends JpaRepository<RestockOffer, Integer> {

    @EntityGraph(attributePaths = {"supplier", "product"})
    List<RestockOffer> findByProduct_ProductIdOrderBySubmittedAtDesc(Integer productId);

    @EntityGraph(attributePaths = {"supplier", "product"})
    List<RestockOffer> findAllByOrderBySubmittedAtDesc();

    @Override
    @EntityGraph(attributePaths = {"supplier", "product"})
    Optional<RestockOffer> findById(Integer id);

    boolean existsBySupplier_SupplierId(Integer supplierId);
}
