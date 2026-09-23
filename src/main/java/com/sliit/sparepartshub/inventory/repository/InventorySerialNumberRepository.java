package com.sliit.sparepartshub.inventory.repository;

import com.sliit.sparepartshub.entity.SerialNumber;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface InventorySerialNumberRepository extends JpaRepository<SerialNumber, Integer> {
    long countByProduct_ProductId(Integer productId);

    long countByProduct_ProductIdAndCurrentStatus(Integer productId, SerialNumber.CurrentStatus status);

    @Query("select count(distinct s.product.productId) from SerialNumber s")
    long countDistinctTrackedProducts();

    @EntityGraph(attributePaths = {"product", "sale"})
    List<SerialNumber> findBySale_SaleIdAndProduct_ProductIdOrderBySerialIdAsc(Integer saleId, Integer productId);
}
