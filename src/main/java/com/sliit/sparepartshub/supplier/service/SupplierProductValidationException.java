package com.sliit.sparepartshub.supplier.service;

/**
 * Validation error for one supplier-product row. The row index is preserved so
 * the Thymeleaf page can highlight the exact product that needs attention.
 */
public class SupplierProductValidationException extends IllegalArgumentException {

    private final int rowIndex;
    private final String field;

    public SupplierProductValidationException(int rowIndex, String field, String message) {
        super(message);
        this.rowIndex = rowIndex;
        this.field = field;
    }

    public int getRowIndex() {
        return rowIndex;
    }

    public String getField() {
        return field;
    }
}
