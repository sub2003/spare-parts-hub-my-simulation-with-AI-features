package com.sliit.sparepartshub.reporting.dto;

import com.sliit.sparepartshub.entity.Sale;
import com.sliit.sparepartshub.entity.SaleItem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class SalesReportData {
    private final LocalDate from;
    private final LocalDate to;
    private final List<Sale> sales;
    private final List<SaleItem> items;
    private final List<TopProductRow> topProducts;
    private final long saleCount;
    private final long quantitySold;
    private final BigDecimal totalRevenue;
    private final BigDecimal averageSaleValue;

    public SalesReportData(LocalDate from,
                           LocalDate to,
                           List<Sale> sales,
                           List<SaleItem> items,
                           List<TopProductRow> topProducts,
                           long saleCount,
                           long quantitySold,
                           BigDecimal totalRevenue,
                           BigDecimal averageSaleValue) {
        this.from = from;
        this.to = to;
        this.sales = sales;
        this.items = items;
        this.topProducts = topProducts;
        this.saleCount = saleCount;
        this.quantitySold = quantitySold;
        this.totalRevenue = totalRevenue;
        this.averageSaleValue = averageSaleValue;
    }

    public static SalesReportData empty(LocalDate from, LocalDate to) {
        return new SalesReportData(from, to, List.of(), List.of(), List.of(), 0, 0,
                BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public LocalDate getFrom() { return from; }
    public LocalDate getTo() { return to; }
    public List<Sale> getSales() { return sales; }
    public List<SaleItem> getItems() { return items; }
    public List<TopProductRow> getTopProducts() { return topProducts; }
    public long getSaleCount() { return saleCount; }
    public long getQuantitySold() { return quantitySold; }
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public BigDecimal getAverageSaleValue() { return averageSaleValue; }
}
