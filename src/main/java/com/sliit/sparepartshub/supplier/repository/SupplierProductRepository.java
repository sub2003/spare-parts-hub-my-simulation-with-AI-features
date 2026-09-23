package com.sliit.sparepartshub.supplier.repository;

import com.sliit.sparepartshub.entity.SupplierProduct;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierProductRepository extends JpaRepository<SupplierProduct, Integer> {

    @EntityGraph(attributePaths = {"supplier", "product"})
    List<SupplierProduct> findByProduct_ProductId(Integer productId);

    @EntityGraph(attributePaths = {"supplier", "product"})
    List<SupplierProduct> findBySupplier_SupplierIdOrderByProduct_Name(Integer supplierId);

    Optional<SupplierProduct> findBySupplier_SupplierIdAndProduct_ProductId(
            Integer supplierId,
            Integer productId);
}
