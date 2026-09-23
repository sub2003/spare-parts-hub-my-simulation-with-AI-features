package com.sliit.sparepartshub.inventory.dto;

import com.sliit.sparepartshub.entity.Product;

public class InventoryProductRow {
    private final Product product;
    private final long totalSerials;
    private final long availableSerials;

    public InventoryProductRow(Product product, long totalSerials, long availableSerials) {
        this.product = product;
        this.totalSerials = totalSerials;
        this.availableSerials = availableSerials;
    }

    public Product getProduct() {
        return product;
    }

    public long getTotalSerials() {
        return totalSerials;
    }

    public long getAvailableSerials() {
        return availableSerials;
    }

    public boolean isSerialTracked() {
        return product.isSerialTracked();
    }

    public boolean isOutOfStock() {
        return product.getStockCount() != null && product.getStockCount() == 0;
    }

    public boolean isLowStock() {
        if (product.getStockCount() == null || product.getReorderLevel() == null) {
            return false;
        }
        return product.getStockCount() > 0 && product.getStockCount() <= product.getReorderLevel();
    }
}
