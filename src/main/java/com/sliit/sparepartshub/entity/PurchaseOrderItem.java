package com.sliit.sparepartshub.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "purchase_order_item")
public class PurchaseOrderItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "po_item_id")
    private Integer poItemId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "po_id", nullable = false)
    private PurchaseOrder purchaseOrder;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    @Column(name = "quantity_ordered", nullable = false)
    private Integer quantityOrdered;
    @Column(name = "received_quantity", nullable = false)
    private Integer receivedQuantity = 0;
    @Column(name = "price_agreed", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAgreed;

    public PurchaseOrderItem() {}

    public Integer getPoItemId() {
        return poItemId;
    }
    public void setPoItemId(Integer poItemId) {
        this.poItemId = poItemId;
    }
    public PurchaseOrder getPurchaseOrder() {
        return purchaseOrder;
    }
    public void setPurchaseOrder(PurchaseOrder purchaseOrder) {
        this.purchaseOrder = purchaseOrder;
    }
    public Product getProduct() {
        return product;
    }
    public void setProduct(Product product) {
        this.product = product;
    }
    public Integer getQuantityOrdered() {
        return quantityOrdered;
    }
    public void setQuantityOrdered(Integer quantityOrdered) {
        this.quantityOrdered = quantityOrdered;
    }
    public Integer getReceivedQuantity() {
        return receivedQuantity == null ? 0 : receivedQuantity;
    }
    public void setReceivedQuantity(Integer receivedQuantity) {
        this.receivedQuantity = receivedQuantity;
    }
    public BigDecimal getPriceAgreed() {
        return priceAgreed;
    }
    public void setPriceAgreed(BigDecimal priceAgreed) {
        this.priceAgreed = priceAgreed;
    }
}
