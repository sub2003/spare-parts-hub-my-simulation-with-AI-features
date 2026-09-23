package com.sliit.sparepartshub.reporting.dto;

public class SupplierVelocityRow {
    private final Integer productId;
    private final String productName;
    private final long unitsSoldLast30Days;

    public SupplierVelocityRow(Integer productId, String productName, long unitsSoldLast30Days) {
        this.productId = productId;
        this.productName = productName;
        this.unitsSoldLast30Days = unitsSoldLast30Days;
    }

    public Integer getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public long getUnitsSoldLast30Days() {
        return unitsSoldLast30Days;
    }
}
