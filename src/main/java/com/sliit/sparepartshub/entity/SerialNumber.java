package com.sliit.sparepartshub.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

/**
 * Tracks an individual physical unit of a product for warranty/RMA
 * purposes (Function 4). Tied to a Sale once sold so a claim can be
 * validated as originally sold by this store.
 */
@Entity
@Table(name = "serial_number")
public class SerialNumber {

    public enum CurrentStatus {
        in_stock, sold, returned, defective, replacement
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "serial_id")
    private Integer serialId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Nullable: only set for units received against a tracked purchase
    // order (UC-05 step 10 - "records delivered quantities, component
    // serial numbers"). Older/manually-entered stock may not have one.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_item_id")
    private PurchaseOrderItem purchaseOrderItem;

    // Nullable: unset until the unit is actually sold.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id")
    private Sale sale;

    @Column(name = "serial_value", nullable = false, unique = true, length = 50)
    private String serialValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false)
    private CurrentStatus currentStatus = CurrentStatus.in_stock;

    @Column(name = "received_date", nullable = false)
    private LocalDate receivedDate;

    public SerialNumber() {
    }

    public Integer getSerialId() {
        return serialId;
    }

    public void setSerialId(Integer serialId) {
        this.serialId = serialId;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public PurchaseOrderItem getPurchaseOrderItem() {
        return purchaseOrderItem;
    }

    public void setPurchaseOrderItem(PurchaseOrderItem purchaseOrderItem) {
        this.purchaseOrderItem = purchaseOrderItem;
    }

    public Sale getSale() {
        return sale;
    }

    public void setSale(Sale sale) {
        this.sale = sale;
    }

    public String getSerialValue() {
        return serialValue;
    }

    public void setSerialValue(String serialValue) {
        this.serialValue = serialValue;
    }

    public CurrentStatus getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(CurrentStatus currentStatus) {
        this.currentStatus = currentStatus;
    }

    public LocalDate getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(LocalDate receivedDate) {
        this.receivedDate = receivedDate;
    }
}
