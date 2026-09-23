package com.sliit.sparepartshub.sales.repository; import com.sliit.sparepartshub.entity.SaleItem;import org.springframework.data.jpa.repository.*;import java.util.*;
public interface SalesSaleItemRepository extends JpaRepository<SaleItem,Integer>{ @EntityGraph(attributePaths="product") List<SaleItem> findBySale_SaleId(Integer saleId); }
