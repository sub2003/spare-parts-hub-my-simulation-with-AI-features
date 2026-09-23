package com.sliit.sparepartshub.inventory.repository;

import com.sliit.sparepartshub.entity.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface InventoryProductRepository extends JpaRepository<Product, Integer> {
    @EntityGraph(attributePaths = "location")
    List<Product> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = "location")
    Optional<Product> findByProductId(Integer productId);

    @EntityGraph(attributePaths = "location")
    @Query("select p from Product p where p.stockCount > 0 and p.stockCount <= p.reorderLevel order by p.stockCount asc, p.name asc")
    List<Product> findLowStockProducts();

    @Query("select count(p) from Product p where p.stockCount > 0 and p.stockCount <= p.reorderLevel")
    long countLowStockProducts();

    long countByStockCount(Integer stockCount);

    long countBySerialTrackedTrue();

    boolean existsByProductCodeIgnoreCase(String productCode);

    boolean existsByProductCodeIgnoreCaseAndProductIdNot(String productCode, Integer productId);

    @Query("select distinct p.category from Product p order by p.category")
    List<String> findDistinctCategories();

    @Query("select distinct p.brand from Product p order by p.brand")
    List<String> findDistinctBrands();
}
