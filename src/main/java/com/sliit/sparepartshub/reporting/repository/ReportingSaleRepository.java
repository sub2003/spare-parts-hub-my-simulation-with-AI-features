package com.sliit.sparepartshub.reporting.repository;

import com.sliit.sparepartshub.entity.Sale;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReportingSaleRepository extends JpaRepository<Sale, Integer> {

    @Query("select coalesce(sum(s.amount),0) from Sale s where s.soldAt>=:from and s.soldAt<:to")
    BigDecimal total(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("select coalesce(sum(s.amount),0) from Sale s")
    BigDecimal totalAll();

    @Query("select min(s.soldAt) from Sale s")
    Optional<LocalDateTime> earliestSoldAt();

    @EntityGraph(attributePaths = "soldBy")
    @Query("select s from Sale s where s.soldAt>=:from and s.soldAt<:to order by s.soldAt desc")
    List<Sale> range(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
