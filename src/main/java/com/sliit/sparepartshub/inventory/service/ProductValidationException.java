package com.sliit.sparepartshub.inventory.service;

public class ProductValidationException extends IllegalArgumentException {
    private final String field;

    public ProductValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
