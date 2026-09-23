package com.sliit.sparepartshub.sales.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class CartLine {
    private Integer productId;
    private String code;
    private String name;
    private Integer quantity;
    /** Final transaction unit price after the per-unit discount. */
    private BigDecimal unitPrice;
    /** Catalog price snapshot reviewed by the cashier for this cart line. */
    private BigDecimal catalogPrice;
    /** Fixed LKR discount applied to each unit in this line. */
    private BigDecimal discountAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private String discountReason;
    private boolean serialTracked;
    private final List<Integer> selectedSerialIds = new ArrayList<>();

    public Integer getProductId() { return productId; }
    public void setProductId(Integer v) { productId = v; }
    public String getCode() { return code; }
    public void setCode(String v) { code = v; }
    public String getName() { return name; }
    public void setName(String v) { name = v; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer v) { quantity = v; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal v) { unitPrice = v == null ? null : money(v); }
    public BigDecimal getCatalogPrice() { return catalogPrice; }
    public void setCatalogPrice(BigDecimal v) { catalogPrice = v == null ? null : money(v); }
    public BigDecimal getDiscountAmount() { return money(discountAmount); }
    public void setDiscountAmount(BigDecimal v) { discountAmount = money(v); }
    public String getDiscountReason() { return discountReason; }
    public void setDiscountReason(String v) { discountReason = v; }
    public boolean isSerialTracked() { return serialTracked; }
    public void setSerialTracked(boolean serialTracked) { this.serialTracked = serialTracked; }
    public List<Integer> getSelectedSerialIds() { return selectedSerialIds; }

    public BigDecimal getLineSubtotal() {
        if (catalogPrice == null || quantity == null) return zeroMoney();
        return money(catalogPrice.multiply(BigDecimal.valueOf(quantity)));
    }

    public BigDecimal getLineDiscount() {
        if (quantity == null) return zeroMoney();
        return money(getDiscountAmount().multiply(BigDecimal.valueOf(quantity)));
    }

    public BigDecimal getLineTotal() {
        if (unitPrice == null || quantity == null) return zeroMoney();
        return money(unitPrice.multiply(BigDecimal.valueOf(quantity)));
    }

    private BigDecimal zeroMoney() {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
