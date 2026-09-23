package com.sliit.sparepartshub.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "pick_ticket_item")
public class PickTicketItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pick_ticket_item_id")
    private Integer pickTicketItemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private PickTicket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    // Actual quantity picked, which may differ from the requested
    // quantity for partial picks or damaged items (UC-01 step 7a).
    // Null until the Warehouse Clerk confirms the pick.
    @Column(name = "picked_quantity")
    private Integer pickedQuantity;

    public PickTicketItem() {
    }

    public Integer getPickTicketItemId() {
        return pickTicketItemId;
    }

    public void setPickTicketItemId(Integer pickTicketItemId) {
        this.pickTicketItemId = pickTicketItemId;
    }

    public PickTicket getTicket() {
        return ticket;
    }

    public void setTicket(PickTicket ticket) {
        this.ticket = ticket;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getPickedQuantity() {
        return pickedQuantity;
    }

    public void setPickedQuantity(Integer pickedQuantity) {
        this.pickedQuantity = pickedQuantity;
    }
}
