package com.sliit.sparepartshub.reporting.repository;

import com.sliit.sparepartshub.entity.SaleItem;
import com.sliit.sparepartshub.reporting.ai.DailyProductSalesProjection;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReportingSaleItemRepository extends JpaRepository<SaleItem, Integer> {

    @Override
    @EntityGraph(attributePaths = {"product", "sale", "sale.soldBy"})
    List<SaleItem> findAll();

    @EntityGraph(attributePaths = {"product", "sale", "sale.soldBy"})
    @Query("select si from SaleItem si where si.sale.soldAt>=:from and si.sale.soldAt<:to order by si.sale.soldAt desc, si.saleItemId asc")
    List<SaleItem> range(@Param("from") LocalDateTime from,
                         @Param("to") LocalDateTime to);

    @Query("select coalesce(sum(si.quantity),0) from SaleItem si")
    Long totalQuantity();

    @Query(value = """
            select date(s.sold_at) as saleDate,
                   si.product_id as productId,
                   sum(si.quantity) as unitsSold
              from sale_item si
              join sale s on s.sale_id = si.sale_id
             group by date(s.sold_at), si.product_id
             order by date(s.sold_at), si.product_id
            """, nativeQuery = true)
    List<DailyProductSalesProjection> dailyProductSales();
}
