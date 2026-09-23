package com.sliit.sparepartshub.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * A supplier's price/terms for supplying a given product. Function 5 uses
 * this to compare vendors (price, MOQ, lead time) and auto-suggest the
 * optimal supplier for a purchase order.
 */
@Entity
@Table(name = "supplier_product")
public class SupplierProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "supplier_product_id")
    private Integer supplierProductId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "moq", nullable = false)
    private Integer moq;

    @Column(name = "lead_time_days", nullable = false)
    private Integer leadTimeDays;

    public SupplierProduct() {
    }

    public Integer getSupplierProductId() {
        return supplierProductId;
    }

    public void setSupplierProductId(Integer supplierProductId) {
        this.supplierProductId = supplierProductId;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getMoq() {
        return moq;
    }

    public void setMoq(Integer moq) {
        this.moq = moq;
    }

    public Integer getLeadTimeDays() {
        return leadTimeDays;
    }

    public void setLeadTimeDays(Integer leadTimeDays) {
        this.leadTimeDays = leadTimeDays;
    }
}
