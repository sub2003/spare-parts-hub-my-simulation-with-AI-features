package com.sliit.sparepartshub.reporting.repository;

import com.sliit.sparepartshub.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReportingPurchaseOrderRepository extends JpaRepository<PurchaseOrder, Integer> {
    @EntityGraph(attributePaths = {"supplier", "createdBy"})
    List<PurchaseOrder> findBySupplier_SupplierIdOrderByCreatedAtDesc(Integer supplierId);

    @EntityGraph(attributePaths = {"supplier", "createdBy"})
    Optional<PurchaseOrder> findByPoIdAndSupplier_SupplierId(Integer poId, Integer supplierId);

    @EntityGraph(attributePaths = {"supplier", "createdBy"})
    List<PurchaseOrder> findTop5ByOrderByCreatedAtDesc();

    long countByStatusIn(Collection<PurchaseOrder.Status> status);
}
