package com.sliit.sparepartshub.warranty.repository;

import com.sliit.sparepartshub.entity.SerialNumber;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WarrantySerialRepository extends JpaRepository<SerialNumber, Integer> {

    @EntityGraph(attributePaths = {"product", "sale", "sale.soldBy"})
    Optional<SerialNumber> findBySerialValueIgnoreCase(String serialValue);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select s
            from SerialNumber s
            join fetch s.product
            left join fetch s.sale
            where s.serialId = :serialId
            """)
    Optional<SerialNumber> findByIdForUpdate(@Param("serialId") Integer serialId);

    @EntityGraph(attributePaths = "product")
    @Query("""
            select s
            from SerialNumber s
            where s.product.productId = :productId
              and s.currentStatus = :status
              and s.sale is null
              and not exists (
                    select c.claimId
                    from RmaClaim c
                    where c.replacementSerial = s
              )
            order by s.serialValue
            """)
    List<SerialNumber> findAvailableReplacementSerials(@Param("productId") Integer productId,
                                                       @Param("status") SerialNumber.CurrentStatus status);
}
