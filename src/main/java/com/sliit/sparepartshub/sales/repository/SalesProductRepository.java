package com.sliit.sparepartshub.sales.repository;

import com.sliit.sparepartshub.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SalesProductRepository extends JpaRepository<Product, Integer> {

    @EntityGraph(attributePaths = "location")
    @Query("""
            select p
            from Product p
            where (
                :q is null or :q = ''
                or lower(p.productCode) like lower(concat('%', :q, '%'))
                or lower(p.name) like lower(concat('%', :q, '%'))
                or lower(p.category) like lower(concat('%', :q, '%'))
                or lower(p.brand) like lower(concat('%', :q, '%'))
            )
            and (:category is null or :category = '' or p.category = :category)
            and (:brand is null or :brand = '' or p.brand = :brand)
            order by p.name
            """)
    List<Product> search(@Param("q") String q,
                         @Param("category") String category,
                         @Param("brand") String brand);

    @Query("select distinct p.category from Product p order by p.category")
    List<String> categories();

    @Query("select distinct p.brand from Product p order by p.brand")
    List<String> brands();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.productId = :productId")
    Optional<Product> findByIdForUpdate(@Param("productId") Integer productId);
}
