package com.sliit.sparepartshub.sales.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class SalesCart {
    private final List<CartLine> lines = new ArrayList<>();
    private String compatibilityOverrideReason;

    public List<CartLine> getLines() { return lines; }
    public String getCompatibilityOverrideReason() { return compatibilityOverrideReason; }
    public void setCompatibilityOverrideReason(String v) { compatibilityOverrideReason = v; }

    public BigDecimal getSubtotal() {
        return money(lines.stream().map(CartLine::getLineSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public BigDecimal getDiscountTotal() {
        return money(lines.stream().map(CartLine::getLineDiscount).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public BigDecimal getTotal() {
        return money(lines.stream().map(CartLine::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public int getItemCount() {
        return lines.stream().mapToInt(x -> x.getQuantity() == null ? 0 : x.getQuantity()).sum();
    }

    public void clear() {
        lines.clear();
        compatibilityOverrideReason = null;
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
