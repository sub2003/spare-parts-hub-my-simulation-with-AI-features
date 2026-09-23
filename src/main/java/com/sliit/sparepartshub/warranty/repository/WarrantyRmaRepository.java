package com.sliit.sparepartshub.warranty.repository;

import com.sliit.sparepartshub.entity.RmaClaim;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WarrantyRmaRepository extends JpaRepository<RmaClaim, Integer> {

    @EntityGraph(attributePaths = {
            "serial", "serial.product", "serial.sale",
            "processedBy", "reviewedBy", "resolvedBy", "replacementSerial"
    })
    List<RmaClaim> findAllByOrderByClaimDateDescClaimIdDesc();

    @EntityGraph(attributePaths = {
            "serial", "serial.product", "serial.sale",
            "processedBy", "reviewedBy", "resolvedBy", "replacementSerial"
    })
    List<RmaClaim> findByClaimStatusOrderByClaimDateDescClaimIdDesc(RmaClaim.ClaimStatus claimStatus);

    @EntityGraph(attributePaths = {
            "serial", "serial.product", "serial.sale",
            "processedBy", "reviewedBy", "resolvedBy", "replacementSerial"
    })
    List<RmaClaim> findBySerial_SerialIdOrderByClaimDateDescClaimIdDesc(Integer serialId);

    @EntityGraph(attributePaths = {
            "serial", "serial.product", "serial.sale", "serial.sale.soldBy",
            "processedBy", "reviewedBy", "resolvedBy", "replacementSerial"
    })
    @Query("select c from RmaClaim c where c.claimId = :claimId")
    Optional<RmaClaim> findDetailedById(@Param("claimId") Integer claimId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"serial", "serial.product", "serial.sale"})
    @Query("select c from RmaClaim c where c.claimId = :claimId")
    Optional<RmaClaim> findByIdForUpdate(@Param("claimId") Integer claimId);

    @EntityGraph(attributePaths = {"serial", "serial.product", "serial.sale"})
    Optional<RmaClaim> findFirstBySerial_SerialIdAndClaimStatusInOrderByClaimIdDesc(
            Integer serialId,
            Collection<RmaClaim.ClaimStatus> statuses
    );

    boolean existsBySerial_SerialIdAndClaimStatusIn(
            Integer serialId,
            Collection<RmaClaim.ClaimStatus> statuses
    );

    boolean existsByReplacementSerial_SerialId(Integer serialId);

    long countByClaimStatus(RmaClaim.ClaimStatus claimStatus);

    long countByClaimDate(LocalDate claimDate);
}
