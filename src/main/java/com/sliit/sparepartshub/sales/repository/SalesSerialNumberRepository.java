package com.sliit.sparepartshub.sales.repository;

import com.sliit.sparepartshub.entity.SerialNumber;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SalesSerialNumberRepository extends JpaRepository<SerialNumber, Integer> {

    @EntityGraph(attributePaths = "product")
    List<SerialNumber> findByProduct_ProductIdAndCurrentStatusAndSaleIsNullOrderBySerialValueAsc(
            Integer productId,
            SerialNumber.CurrentStatus status
    );

    long countByProduct_ProductIdAndCurrentStatusAndSaleIsNull(
            Integer productId,
            SerialNumber.CurrentStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select s
            from SerialNumber s
            join fetch s.product
            where s.serialId in :serialIds
            order by s.serialId
            """)
    List<SerialNumber> findAllForUpdate(@Param("serialIds") Collection<Integer> serialIds);

    @Query("""
            select s
            from SerialNumber s
            join fetch s.product
            where s.sale.saleId = :saleId
            order by s.product.name, s.serialValue
            """)
    List<SerialNumber> findBySaleIdWithProduct(@Param("saleId") Integer saleId);
}
