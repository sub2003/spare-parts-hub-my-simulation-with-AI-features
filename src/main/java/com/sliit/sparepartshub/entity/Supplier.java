package com.sliit.sparepartshub.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "supplier")
public class Supplier {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "supplier_id")
    private Integer supplierId;

    @Column(name = "supplier_code", length = 20)
    private String supplierCode;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "contact", nullable = false, length = 20)
    private String contact;

    @Column(name = "email", nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 60)
    private String passwordHash;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    public Supplier() {}
    public Integer getSupplierId() { return supplierId; }
    public void setSupplierId(Integer supplierId) { this.supplierId = supplierId; }
    public String getSupplierCode() { return supplierCode; }
    public void setSupplierCode(String supplierCode) { this.supplierCode = supplierCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Boolean getActive() { return active; }
    public boolean isActive() { return Boolean.TRUE.equals(active); }
    public void setActive(Boolean active) { this.active = active; }
}
