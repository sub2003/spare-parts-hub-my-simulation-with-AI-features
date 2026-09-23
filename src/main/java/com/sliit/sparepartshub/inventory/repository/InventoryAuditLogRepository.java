package com.sliit.sparepartshub.inventory.repository;
import com.sliit.sparepartshub.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
public interface InventoryAuditLogRepository extends JpaRepository<AuditLog,Integer> {}
