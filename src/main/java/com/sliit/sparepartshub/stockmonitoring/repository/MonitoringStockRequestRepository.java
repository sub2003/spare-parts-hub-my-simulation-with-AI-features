package com.sliit.sparepartshub.stockmonitoring.repository;

import com.sliit.sparepartshub.entity.StockRequest;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MonitoringStockRequestRepository extends JpaRepository<StockRequest, Integer> {

    @EntityGraph(attributePaths = {"product", "loggedBy"})
    List<StockRequest> findAllByOrderByRequestedAtDesc();

    @EntityGraph(attributePaths = {"product", "loggedBy"})
    List<StockRequest> findByProduct_ProductIdAndStatusOrderByRequestedAtAsc(
            Integer productId,
            StockRequest.Status status
    );

    @Query("""
            select coalesce(sum(r.requestedQuantity), 0)
            from StockRequest r
            where r.product.productId = :pid
              and r.status <> :closedStatus
            """)
    Long openDemand(@Param("pid") Integer productId,
                    @Param("closedStatus") StockRequest.Status closedStatus);

    @Query("""
            select coalesce(sum(r.requestedQuantity), 0)
            from StockRequest r
            where r.product.productId = :pid
              and r.status in :statuses
            """)
    Long demandByStatuses(@Param("pid") Integer productId,
                          @Param("statuses") List<StockRequest.Status> statuses);

    long countByStatus(StockRequest.Status status);

    long countByStatusNot(StockRequest.Status status);
}
