package com.sliit.sparepartshub.inventory.dto;

import java.math.BigDecimal;

public class EditProductForm {
    private String productCode;
    private String name;
    private String category;
    private String brand;
    private BigDecimal price;
    private Integer reorderLevel;
    private Integer warrantyPeriodMonths;
    private boolean serialTracked;
    private Integer locationId;

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(Integer reorderLevel) { this.reorderLevel = reorderLevel; }
    public Integer getWarrantyPeriodMonths() { return warrantyPeriodMonths; }
    public void setWarrantyPeriodMonths(Integer warrantyPeriodMonths) { this.warrantyPeriodMonths = warrantyPeriodMonths; }
    public boolean isSerialTracked() { return serialTracked; }
    public void setSerialTracked(boolean serialTracked) { this.serialTracked = serialTracked; }
    public Integer getLocationId() { return locationId; }
    public void setLocationId(Integer locationId) { this.locationId = locationId; }
}
