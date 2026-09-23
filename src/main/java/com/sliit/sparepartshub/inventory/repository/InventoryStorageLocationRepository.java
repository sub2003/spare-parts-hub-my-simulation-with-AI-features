package com.sliit.sparepartshub.inventory.repository;

import com.sliit.sparepartshub.entity.StorageLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InventoryStorageLocationRepository extends JpaRepository<StorageLocation, Integer> {
    List<StorageLocation> findAllByOrderByLocationCodeAsc();
    boolean existsByLocationCodeIgnoreCase(String locationCode);
    boolean existsByLocationCodeIgnoreCaseAndLocationIdNot(String locationCode, Integer locationId);
}
