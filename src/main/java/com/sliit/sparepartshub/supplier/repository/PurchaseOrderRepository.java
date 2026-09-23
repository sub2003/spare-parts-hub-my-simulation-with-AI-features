package com.sliit.sparepartshub.supplier.repository;

import com.sliit.sparepartshub.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Integer> {

    @EntityGraph(attributePaths = {"supplier", "createdBy"})
    List<PurchaseOrder> findAllByOrderByCreatedAtDesc();

    @Override
    @EntityGraph(attributePaths = {"supplier", "createdBy"})
    Optional<PurchaseOrder> findById(Integer id);

    boolean existsBySupplier_SupplierId(Integer supplierId);
}
