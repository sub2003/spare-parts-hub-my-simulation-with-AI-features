-- Final non-destructive integrity checks for Spare Parts Hub
-- Expected result for checks 1-10: zero rows.
-- Check 11 is diagnostic for serial-tracked legacy/demo stock reconciliation.
USE spareparts_mysimulation;

-- 1. No negative product stock.
SELECT product_id, product_code, name, stock_count
FROM product
WHERE stock_count < 0;

-- 2. No orphan SaleItem rows.
SELECT si.*
FROM sale_item si
LEFT JOIN sale s ON s.sale_id = si.sale_id
LEFT JOIN product p ON p.product_id = si.product_id
WHERE s.sale_id IS NULL OR p.product_id IS NULL;

-- 3. No orphan PickTicketItem rows.
SELECT pti.*
FROM pick_ticket_item pti
LEFT JOIN pick_ticket pt ON pt.ticket_id = pti.ticket_id
LEFT JOIN product p ON p.product_id = pti.product_id
WHERE pt.ticket_id IS NULL OR p.product_id IS NULL;

-- 4. No duplicate physical serial values.
SELECT serial_value, COUNT(*) AS occurrences
FROM serial_number
GROUP BY serial_value
HAVING COUNT(*) > 1;

-- 5. A serial linked to a Sale must not still be logically in stock.
SELECT serial_id, serial_value, sale_id, current_status
FROM serial_number
WHERE sale_id IS NOT NULL AND current_status = 'in_stock';

-- 6. A serial marked sold must have a Sale link.
SELECT serial_id, serial_value, sale_id, current_status
FROM serial_number
WHERE current_status = 'sold' AND sale_id IS NULL;

-- 7. A replacement serial must not be reused by multiple RMAs.
SELECT replacement_serial_id, COUNT(*) AS rma_count
FROM rma_claim
WHERE replacement_serial_id IS NOT NULL
GROUP BY replacement_serial_id
HAVING COUNT(*) > 1;

-- 8. No duplicate open RMA for the same original serial.
SELECT serial_id, COUNT(*) AS open_claims
FROM rma_claim
WHERE claim_status IN ('pending','approved')
GROUP BY serial_id
HAVING COUNT(*) > 1;

-- 9. PO received quantity must never exceed ordered quantity or be negative.
SELECT po_item_id, po_id, product_id, quantity_ordered, received_quantity
FROM purchase_order_item
WHERE received_quantity < 0 OR received_quantity > quantity_ordered;

-- 10. AuditReview must always reference an existing audit event and reviewer.
SELECT ar.*
FROM audit_review ar
LEFT JOIN audit_log al ON al.log_id = ar.audit_log_id
LEFT JOIN users u ON u.user_id = ar.reviewed_by
WHERE al.log_id IS NULL OR u.user_id IS NULL;

-- 11. Diagnostic only: serial-tracked stock should ideally equal the number
-- of sellable in_stock physical serials. Historical/demo seed data may expose
-- legacy mismatches; the application still limits POS quantity by valid serials.
SELECT p.product_id,
       p.product_code,
       p.name,
       p.stock_count,
       SUM(CASE WHEN sn.current_status = 'in_stock' AND sn.sale_id IS NULL THEN 1 ELSE 0 END) AS available_serials
FROM product p
LEFT JOIN serial_number sn ON sn.product_id = p.product_id
WHERE p.serial_tracked = TRUE
GROUP BY p.product_id, p.product_code, p.name, p.stock_count
HAVING p.stock_count <> available_serials;
