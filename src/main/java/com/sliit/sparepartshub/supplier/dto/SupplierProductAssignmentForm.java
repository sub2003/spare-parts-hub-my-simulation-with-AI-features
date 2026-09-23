package com.sliit.sparepartshub.supplier.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin form for defining which products a supplier can supply and the
 * supplier-specific commercial terms used by comparison, purchase-order and
 * Supplier Portal offer flows.
 */
public class SupplierProductAssignmentForm {

    private List<SupplierProductRowForm> rows = new ArrayList<>();

    public List<SupplierProductRowForm> getRows() {
        return rows;
    }

    public void setRows(List<SupplierProductRowForm> rows) {
        this.rows = rows == null ? new ArrayList<>() : rows;
    }
}
