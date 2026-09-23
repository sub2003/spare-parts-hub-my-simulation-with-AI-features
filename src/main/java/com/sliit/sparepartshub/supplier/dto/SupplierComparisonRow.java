package com.sliit.sparepartshub.supplier.dto;

import java.math.BigDecimal;

public class SupplierComparisonRow {
    private Integer supplierId;
    private String supplierName;
    private BigDecimal unitPrice;
    private Integer moq;
    private Integer leadTimeDays;
    private boolean offerUsed;
    private Integer offerQuantity;
    private BigDecimal totalCost;
    private boolean eligible;
    private boolean recommended;

    public Integer getSupplierId() { return supplierId; }
    public void setSupplierId(Integer supplierId) { this.supplierId = supplierId; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public Integer getMoq() { return moq; }
    public void setMoq(Integer moq) { this.moq = moq; }
    public Integer getLeadTimeDays() { return leadTimeDays; }
    public void setLeadTimeDays(Integer leadTimeDays) { this.leadTimeDays = leadTimeDays; }
    public boolean isOfferUsed() { return offerUsed; }
    public void setOfferUsed(boolean offerUsed) { this.offerUsed = offerUsed; }
    public Integer getOfferQuantity() { return offerQuantity; }
    public void setOfferQuantity(Integer offerQuantity) { this.offerQuantity = offerQuantity; }
    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
    public boolean isEligible() { return eligible; }
    public void setEligible(boolean eligible) { this.eligible = eligible; }
    public boolean isRecommended() { return recommended; }
    public void setRecommended(boolean recommended) { this.recommended = recommended; }
}
