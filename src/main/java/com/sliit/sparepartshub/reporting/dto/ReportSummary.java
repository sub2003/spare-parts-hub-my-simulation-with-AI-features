package com.sliit.sparepartshub.reporting.dto;

import java.math.BigDecimal;

public class ReportSummary {
    private BigDecimal todaySales = BigDecimal.ZERO;
    private BigDecimal weekSales = BigDecimal.ZERO;
    private BigDecimal totalRevenue = BigDecimal.ZERO;
    private long totalSales;
    private long totalQuantitySold;
    private long openPos;
    private long pendingRmas;
    private long criticalProducts;

    public BigDecimal getTodaySales() { return todaySales; }
    public void setTodaySales(BigDecimal todaySales) { this.todaySales = todaySales; }
    public BigDecimal getWeekSales() { return weekSales; }
    public void setWeekSales(BigDecimal weekSales) { this.weekSales = weekSales; }
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
    public long getTotalSales() { return totalSales; }
    public void setTotalSales(long totalSales) { this.totalSales = totalSales; }
    public long getTotalQuantitySold() { return totalQuantitySold; }
    public void setTotalQuantitySold(long totalQuantitySold) { this.totalQuantitySold = totalQuantitySold; }
    public long getOpenPos() { return openPos; }
    public void setOpenPos(long openPos) { this.openPos = openPos; }
    public long getPendingRmas() { return pendingRmas; }
    public void setPendingRmas(long pendingRmas) { this.pendingRmas = pendingRmas; }
    public long getCriticalProducts() { return criticalProducts; }
    public void setCriticalProducts(long criticalProducts) { this.criticalProducts = criticalProducts; }
}
