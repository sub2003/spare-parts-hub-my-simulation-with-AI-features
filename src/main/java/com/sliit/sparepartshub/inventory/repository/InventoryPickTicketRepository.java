package com.sliit.sparepartshub.inventory.repository;

import com.sliit.sparepartshub.entity.PickTicket;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InventoryPickTicketRepository extends JpaRepository<PickTicket, Integer> {
    @Override
    @EntityGraph(attributePaths = {"sale", "sale.soldBy", "fulfilledBy"})
    Optional<PickTicket> findById(Integer id);

    @EntityGraph(attributePaths = {"sale", "sale.soldBy", "fulfilledBy"})
    List<PickTicket> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"sale", "sale.soldBy", "fulfilledBy"})
    Optional<PickTicket> findByTicketCodeIgnoreCase(String ticketCode);

    long countByStatus(PickTicket.Status status);

    long countByFulfilledAtBetween(LocalDateTime start, LocalDateTime end);

    @EntityGraph(attributePaths = {"sale", "sale.soldBy", "fulfilledBy"})
    List<PickTicket> findTop5ByStatusInOrderByFulfilledAtDesc(Collection<PickTicket.Status> statuses);
}
