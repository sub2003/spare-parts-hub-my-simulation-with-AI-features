package com.sliit.sparepartshub.supplier.service;

/**
 * Validation error tied to a single supplier form field so the controller can
 * render the message next to the corresponding input.
 */
public class SupplierFieldValidationException extends IllegalArgumentException {

    private final String field;

    public SupplierFieldValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
