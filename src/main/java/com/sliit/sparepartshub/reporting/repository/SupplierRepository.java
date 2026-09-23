package com.sliit.sparepartshub.reporting.repository;

import com.sliit.sparepartshub.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Shared repository for Supplier. Supplier Portal authentication and Supplier
 * Management deliberately reuse this one repository so Spring does not create
 * duplicate repository beans for the same aggregate.
 */
public interface SupplierRepository extends JpaRepository<Supplier, Integer> {

    Optional<Supplier> findByEmail(String email);

    Optional<Supplier> findByEmailIgnoreCase(String email);

    List<Supplier> findAllByOrderByNameAsc();

    List<Supplier> findByActiveTrueOrderByNameAsc();

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndSupplierIdNot(String email, Integer supplierId);

    boolean existsBySupplierCodeIgnoreCase(String supplierCode);

    boolean existsBySupplierCodeIgnoreCaseAndSupplierIdNot(String supplierCode, Integer supplierId);
}
