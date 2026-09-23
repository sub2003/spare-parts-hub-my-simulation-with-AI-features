-- Supplier Portal Integration patch for an EXISTING spareparts_mysimulation database.
-- Run once after taking a backup. If a column/index already exists, skip that statement.
USE spareparts_mysimulation;

ALTER TABLE partnership_request ADD COLUMN supplier_id INT NULL AFTER request_id;
ALTER TABLE partnership_request ADD CONSTRAINT fk_partnership_supplier FOREIGN KEY (supplier_id) REFERENCES supplier(supplier_id);
CREATE INDEX idx_partnership_supplier ON partnership_request(supplier_id);

ALTER TABLE purchase_order ADD COLUMN tracking_reference VARCHAR(100) NULL AFTER received_at;
ALTER TABLE purchase_order ADD COLUMN supplier_shipment_note VARCHAR(500) NULL AFTER tracking_reference;
ALTER TABLE purchase_order ADD COLUMN supplier_dispatched_at DATETIME NULL AFTER supplier_shipment_note;
CREATE INDEX idx_purchase_order_supplier ON purchase_order(supplier_id);

-- Existing partnership records are intentionally left with supplier_id NULL unless you can confidently map them.
-- New Supplier Portal submissions always store the authenticated supplier_id.
