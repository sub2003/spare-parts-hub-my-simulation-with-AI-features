package com.sliit.sparepartshub.supplier.repository;

import com.sliit.sparepartshub.entity.SerialNumber;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SerialNumberRepository extends JpaRepository<SerialNumber, Integer> {
    boolean existsBySerialValue(String serialValue);
}
