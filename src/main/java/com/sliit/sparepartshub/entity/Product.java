package com.sliit.sparepartshub.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Integer productId;

    @Column(name = "product_code", nullable = false, unique = true, length = 20)
    private String productCode;

    // Nullable: a product can exist before it has been assigned a physical
    // storage bin (schema does not mark location_id NOT NULL).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private StorageLocation location;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "category", nullable = false, length = 30)
    private String category;

    @Column(name = "brand", nullable = false, length = 30)
    private String brand;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "stock_count", nullable = false)
    private Integer stockCount = 0;

    // Configurable warehouse threshold used by Function 1 to identify low stock.
    // Keeping this on Product avoids a hard-coded global threshold and lets each
    // spare-part type have an appropriate reorder point.
    @Column(name = "reorder_level", nullable = false)
    private Integer reorderLevel = 5;

    // Computed by Function 3 (Dynamic Urgency Score Tracking) from stock
    // level vs. recent sales velocity - not meant to be hand-edited by users.
    @Column(name = "urgency_score", precision = 6, scale = 2)
    private BigDecimal urgencyScore = BigDecimal.ZERO;

    @Column(name = "warranty_period_months")
    private Integer warrantyPeriodMonths;

    // Explicit product-level switch. Previously Function 1 inferred serial
    // tracking from whether historical serial rows existed, which cannot
    // represent a brand-new serial-tracked product before its first receipt.
    @Column(name = "serial_tracked", nullable = false)
    private boolean serialTracked = false;

    public Product() {
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public StorageLocation getLocation() {
        return location;
    }

    public void setLocation(StorageLocation location) {
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStockCount() {
        return stockCount;
    }

    public void setStockCount(Integer stockCount) {
        this.stockCount = stockCount;
    }

    public Integer getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(Integer reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    public BigDecimal getUrgencyScore() {
        return urgencyScore;
    }

    public void setUrgencyScore(BigDecimal urgencyScore) {
        this.urgencyScore = urgencyScore;
    }

    public Integer getWarrantyPeriodMonths() {
        return warrantyPeriodMonths;
    }

    public void setWarrantyPeriodMonths(Integer warrantyPeriodMonths) {
        this.warrantyPeriodMonths = warrantyPeriodMonths;
    }

    public boolean isSerialTracked() {
        return serialTracked;
    }

    public void setSerialTracked(boolean serialTracked) {
        this.serialTracked = serialTracked;
    }
}
