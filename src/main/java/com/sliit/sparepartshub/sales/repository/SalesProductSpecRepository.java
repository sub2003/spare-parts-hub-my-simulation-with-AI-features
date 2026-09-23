package com.sliit.sparepartshub.sales.repository; import com.sliit.sparepartshub.entity.ProductSpec;import org.springframework.data.jpa.repository.*;import java.util.*;
public interface SalesProductSpecRepository extends JpaRepository<ProductSpec,Integer>{ @EntityGraph(attributePaths="product") List<ProductSpec> findByProduct_ProductIdIn(Collection<Integer> ids); }
