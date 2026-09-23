package com.sliit.sparepartshub.stockmonitoring.dto;

import com.sliit.sparepartshub.entity.Product;

import java.math.BigDecimal;

/**
 * Read model for the Function 3 dashboard. The persisted value is
 * Product.urgencyScore; the other values are calculated from live operational
 * data so the score remains explainable to the Inventory Supervisor.
 */
public class UrgencyRow {
    private final Product product;
    private final long sold30;
    private final BigDecimal dailySalesVelocity;
    private final long demand;
    private final long incoming;
    private final int suggestedQuantity;
    private final String classification;

    public UrgencyRow(Product product,
                      long sold30,
                      BigDecimal dailySalesVelocity,
                      long demand,
                      long incoming,
                      int suggestedQuantity,
                      String classification) {
        this.product = product;
        this.sold30 = sold30;
        this.dailySalesVelocity = dailySalesVelocity;
        this.demand = demand;
        this.incoming = incoming;
        this.suggestedQuantity = suggestedQuantity;
        this.classification = classification;
    }

    public Product getProduct() {
        return product;
    }

    public long getSold30() {
        return sold30;
    }

    public BigDecimal getDailySalesVelocity() {
        return dailySalesVelocity;
    }

    public long getDemand() {
        return demand;
    }

    public long getIncoming() {
        return incoming;
    }

    public int getSuggestedQuantity() {
        return suggestedQuantity;
    }

    public String getClassification() {
        return classification;
    }
}
