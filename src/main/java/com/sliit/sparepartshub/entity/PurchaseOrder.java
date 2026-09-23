package com.sliit.sparepartshub.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "purchase_order")
public class PurchaseOrder {
    public enum Status { pending, shipped, partially_received, received }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "po_id")
    private Integer poId;

    @Column(name = "po_code", length = 20)
    private String poCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.pending;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "tracking_reference", length = 100)
    private String trackingReference;

    @Column(name = "supplier_shipment_note", length = 500)
    private String supplierShipmentNote;

    @Column(name = "supplier_dispatched_at")
    private LocalDateTime supplierDispatchedAt;

    public PurchaseOrder() {}

    public Integer getPoId() { return poId; }
    public void setPoId(Integer poId) { this.poId = poId; }
    public String getPoCode() { return poCode; }
    public void setPoCode(String poCode) { this.poCode = poCode; }
    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier supplier) { this.supplier = supplier; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getShippedAt() { return shippedAt; }
    public void setShippedAt(LocalDateTime shippedAt) { this.shippedAt = shippedAt; }
    public LocalDate getExpectedDeliveryDate() { return expectedDeliveryDate; }
    public void setExpectedDeliveryDate(LocalDate expectedDeliveryDate) { this.expectedDeliveryDate = expectedDeliveryDate; }
    public LocalDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }
    public String getTrackingReference() { return trackingReference; }
    public void setTrackingReference(String trackingReference) { this.trackingReference = trackingReference; }
    public String getSupplierShipmentNote() { return supplierShipmentNote; }
    public void setSupplierShipmentNote(String supplierShipmentNote) { this.supplierShipmentNote = supplierShipmentNote; }
    public LocalDateTime getSupplierDispatchedAt() { return supplierDispatchedAt; }
    public void setSupplierDispatchedAt(LocalDateTime supplierDispatchedAt) { this.supplierDispatchedAt = supplierDispatchedAt; }
}
