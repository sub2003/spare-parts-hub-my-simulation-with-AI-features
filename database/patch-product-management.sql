-- Product Management upgrade for an existing spareparts_mysimulation database.
-- Run ONCE after patch-inventory-reorder-level.sql.
USE spareparts_mysimulation;

-- Explicitly records whether a product requires one serial number per physical unit.
-- Run this patch once on the existing simulation database.
ALTER TABLE product
    ADD COLUMN serial_tracked BOOLEAN NOT NULL DEFAULT FALSE
    AFTER warranty_period_months;

-- Preserve the meaning of existing serial-number data: any product that already
-- has serial_number rows is considered serial-tracked.
UPDATE product p
SET p.serial_tracked = TRUE
WHERE EXISTS (
    SELECT 1
    FROM serial_number s
    WHERE s.product_id = p.product_id
);

-- New products are validated in the application, and this index also protects
-- against concurrent duplicate product-code inserts. If legacy duplicate codes
-- exist, the patch skips this index rather than destroying data.
SET @duplicate_product_codes := (
    SELECT COUNT(*)
    FROM (
        SELECT product_code
        FROM product
        WHERE product_code IS NOT NULL
        GROUP BY product_code
        HAVING COUNT(*) > 1
    ) d
);

SET @product_code_index_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'product'
      AND column_name = 'product_code'
      AND non_unique = 0
);

SET @product_code_index_sql := IF(
    @product_code_index_exists = 0 AND @duplicate_product_codes = 0,
    'ALTER TABLE product ADD UNIQUE INDEX uq_product_code (product_code)',
    'SELECT ''Product-code unique index already exists or was skipped because legacy duplicates exist.'' AS product_patch_note'
);

PREPARE product_code_stmt FROM @product_code_index_sql;
EXECUTE product_code_stmt;
DEALLOCATE PREPARE product_code_stmt;
