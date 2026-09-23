package com.sliit.sparepartshub.supplier.repository;

import com.sliit.sparepartshub.entity.PurchaseOrderItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Integer> {

    @EntityGraph(attributePaths = {"product", "purchaseOrder"})
    List<PurchaseOrderItem> findByPurchaseOrder_PoId(Integer poId);

    /**
     * RestockOffer has no explicit accepted/used status in the current schema.
     * A matching PO item is therefore the strongest persisted evidence that an
     * offer was actually used: same supplier/product/price, an ordered quantity
     * within the quoted quantity, and a PO created during the offer window.
     */
    @Query("""
            select count(i)
            from PurchaseOrderItem i
            where i.purchaseOrder.supplier.supplierId = :supplierId
              and i.product.productId = :productId
              and i.priceAgreed = :price
              and i.quantityOrdered <= :offerQuantity
              and i.purchaseOrder.createdAt >= :submittedAt
            """)
    long countMatchingOfferUsageNoExpiry(
            @Param("supplierId") Integer supplierId,
            @Param("productId") Integer productId,
            @Param("price") BigDecimal price,
            @Param("offerQuantity") Integer offerQuantity,
            @Param("submittedAt") LocalDateTime submittedAt);

    @Query("""
            select count(i)
            from PurchaseOrderItem i
            where i.purchaseOrder.supplier.supplierId = :supplierId
              and i.product.productId = :productId
              and i.priceAgreed = :price
              and i.quantityOrdered <= :offerQuantity
              and i.purchaseOrder.createdAt >= :submittedAt
              and i.purchaseOrder.createdAt <= :validUntilEnd
            """)
    long countMatchingOfferUsageWithExpiry(
            @Param("supplierId") Integer supplierId,
            @Param("productId") Integer productId,
            @Param("price") BigDecimal price,
            @Param("offerQuantity") Integer offerQuantity,
            @Param("submittedAt") LocalDateTime submittedAt,
            @Param("validUntilEnd") LocalDateTime validUntilEnd);
}
