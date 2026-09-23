package com.sliit.sparepartshub.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "sale_item")
public class SaleItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "sale_item_id")
    private Integer saleItemId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    @Column(name = "quantity", nullable = false)
    private Integer quantity;
    /** Final unit price actually charged for this historical transaction. */
    @Column(name = "price_at_sale", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAtSale;
    /** Fixed per-unit LKR discount used to derive price_at_sale. */
    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    @Column(name = "compatibility_override_reason", length = 255)
    private String compatibilityOverrideReason;
    @Column(name = "discount_reason", length = 255)
    private String discountReason;

    public SaleItem() {}
    public Integer getSaleItemId() { return saleItemId; }
    public void setSaleItemId(Integer saleItemId) { this.saleItemId = saleItemId; }
    public Sale getSale() { return sale; }
    public void setSale(Sale sale) { this.sale = sale; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getPriceAtSale() { return money(priceAtSale); }
    public void setPriceAtSale(BigDecimal priceAtSale) { this.priceAtSale = money(priceAtSale); }
    public BigDecimal getDiscountAmount() { return money(discountAmount); }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = money(discountAmount); }
    public String getCompatibilityOverrideReason() { return compatibilityOverrideReason; }
    public void setCompatibilityOverrideReason(String compatibilityOverrideReason) { this.compatibilityOverrideReason = compatibilityOverrideReason; }
    public String getDiscountReason() { return discountReason; }
    public void setDiscountReason(String discountReason) { this.discountReason = discountReason; }

    public BigDecimal getOriginalUnitPrice() {
        return money(getPriceAtSale().add(getDiscountAmount()));
    }

    public BigDecimal getLineTotal() {
        return money(getPriceAtSale().multiply(BigDecimal.valueOf(quantity == null ? 0 : quantity)));
    }

    public BigDecimal getOriginalLineTotal() {
        return money(getOriginalUnitPrice().multiply(BigDecimal.valueOf(quantity == null ? 0 : quantity)));
    }

    public BigDecimal getLineDiscountTotal() {
        return money(getDiscountAmount().multiply(BigDecimal.valueOf(quantity == null ? 0 : quantity)));
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
