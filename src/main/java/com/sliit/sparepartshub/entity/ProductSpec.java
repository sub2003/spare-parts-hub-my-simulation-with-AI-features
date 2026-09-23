package com.sliit.sparepartshub.entity;

import jakarta.persistence.*;

/**
 * Individual spec attributes for a product (e.g. spec_type="socket",
 * spec_value="LGA1700"). Cross-referenced against CompatibilityRule at
 * checkout to flag conflicting hardware in the cart.
 */
@Entity
@Table(name = "product_spec")
public class ProductSpec {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_spec_id")
    private Integer productSpecId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "spec_type", nullable = false, length = 30)
    private String specType;

    @Column(name = "spec_value", nullable = false, length = 30)
    private String specValue;

    public ProductSpec() {
    }

    public Integer getProductSpecId() {
        return productSpecId;
    }

    public void setProductSpecId(Integer productSpecId) {
        this.productSpecId = productSpecId;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public String getSpecType() {
        return specType;
    }

    public void setSpecType(String specType) {
        this.specType = specType;
    }

    public String getSpecValue() {
        return specValue;
    }

    public void setSpecValue(String specValue) {
        this.specValue = specValue;
    }
}
