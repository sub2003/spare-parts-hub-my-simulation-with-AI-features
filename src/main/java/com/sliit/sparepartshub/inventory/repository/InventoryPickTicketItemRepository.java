package com.sliit.sparepartshub.inventory.repository;
import com.sliit.sparepartshub.entity.PickTicketItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface InventoryPickTicketItemRepository extends JpaRepository<PickTicketItem,Integer> {
    @EntityGraph(attributePaths={"product","product.location","ticket"})
    List<PickTicketItem> findByTicket_TicketIdOrderByPickTicketItemId(Integer ticketId);
}
