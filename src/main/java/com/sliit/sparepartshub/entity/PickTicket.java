package com.sliit.sparepartshub.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * The QR-coded ticket generated when a Sales Executive requests an item,
 * used by the Warehouse Clerk to locate and pick stock (Function 1).
 */
@Entity
@Table(name = "pick_ticket")
public class PickTicket {

    // Widened from (pending, fulfilled) to match UC-01's documented
    // outcomes: a pick can finish Completed, Partially Completed, or
    // hit an Exception (damaged/missing item), not just a binary done/not.
    public enum Status {
        pending, completed, partially_completed, exception
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id")
    private Integer ticketId;

    @Column(name = "ticket_code", length = 20)
    private String ticketCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    // Nullable until a Warehouse Clerk picks up and fulfills the ticket.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fulfilled_by")
    private User fulfilledBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.pending;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "fulfilled_at")
    private LocalDateTime fulfilledAt;

    public PickTicket() {
    }

    public Integer getTicketId() {
        return ticketId;
    }

    public void setTicketId(Integer ticketId) {
        this.ticketId = ticketId;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public void setTicketCode(String ticketCode) {
        this.ticketCode = ticketCode;
    }

    public Sale getSale() {
        return sale;
    }

    public void setSale(Sale sale) {
        this.sale = sale;
    }

    public User getFulfilledBy() {
        return fulfilledBy;
    }

    public void setFulfilledBy(User fulfilledBy) {
        this.fulfilledBy = fulfilledBy;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getFulfilledAt() {
        return fulfilledAt;
    }

    public void setFulfilledAt(LocalDateTime fulfilledAt) {
        this.fulfilledAt = fulfilledAt;
    }
}
