package com.sliit.sparepartshub.supplier.dto;

/**
 * Form model for Admin-managed supplier account creation.
 *
 * This intentionally excludes internal identifiers and password hashes so the
 * Supplier entity is never bound directly to untrusted form input.
 */
public class CreateSupplierRequest {

    private String supplierCode;
    private String name;
    private String email;
    private String contact;
    private String temporaryPassword;
    private String confirmPassword;
    private boolean active = true;

    public String getSupplierCode() {
        return supplierCode;
    }

    public void setSupplierCode(String supplierCode) {
        this.supplierCode = supplierCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getTemporaryPassword() {
        return temporaryPassword;
    }

    public void setTemporaryPassword(String temporaryPassword) {
        this.temporaryPassword = temporaryPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
