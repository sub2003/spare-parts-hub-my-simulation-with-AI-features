package com.sliit.sparepartshub.sales.repository; import com.sliit.sparepartshub.entity.Sale;import org.springframework.data.jpa.repository.*;import java.util.*;
public interface SalesSaleRepository extends JpaRepository<Sale,Integer>{ @Override @EntityGraph(attributePaths="soldBy") Optional<Sale> findById(Integer saleId); }
