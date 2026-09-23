package com.sliit.sparepartshub.reporting.repository;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class StaffHistoryRepository {

    private final EntityManager entityManager;

    public StaffHistoryRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<String> blockingHistory(Integer userId) {
        List<String> blockers = new ArrayList<>();

        if (exists("select count(s) from Sale s where s.soldBy.userId = :userId", userId)) {
            blockers.add("sales");
        }
        if (exists("select count(p) from PickTicket p where p.fulfilledBy.userId = :userId", userId)) {
            blockers.add("pick-ticket history");
        }
        if (exists("select count(r) from StockRequest r where r.loggedBy.userId = :userId", userId)) {
            blockers.add("stock requests");
        }
        if (exists("select count(p) from PurchaseOrder p where p.createdBy.userId = :userId", userId)) {
            blockers.add("purchase orders");
        }
        if (exists("select count(p) from PartnershipRequest p where p.reviewedBy.userId = :userId", userId)) {
            blockers.add("partnership reviews");
        }
        if (exists("select count(r) from RestockSuggestion r where r.reviewedBy.userId = :userId", userId)) {
            blockers.add("restock reviews");
        }
        if (exists("select count(r) from RmaClaim r where r.processedBy.userId = :userId or r.reviewedBy.userId = :userId or r.resolvedBy.userId = :userId", userId)) {
            blockers.add("warranty/RMA history");
        }
        if (exists("select count(a) from AuditLog a where a.user.userId = :userId", userId)) {
            blockers.add("audit history");
        }
        if (exists("select count(a) from AuditReview a where a.reviewedBy.userId = :userId", userId)) {
            blockers.add("audit reviews");
        }

        return blockers;
    }

    private boolean exists(String jpql, Integer userId) {
        Long count = entityManager.createQuery(jpql, Long.class)
                .setParameter("userId", userId)
                .getSingleResult();
        return count != null && count > 0;
    }
}
