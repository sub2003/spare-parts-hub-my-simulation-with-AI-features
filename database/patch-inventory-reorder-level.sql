USE spareparts_mysimulation;

-- Function 1 inventory completion: configurable per-product low-stock threshold.
-- Safe to run against the existing simulation database before starting the updated app.
SET @reorder_level_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'product'
      AND COLUMN_NAME = 'reorder_level'
);

SET @reorder_level_sql = IF(
    @reorder_level_exists = 0,
    'ALTER TABLE product ADD COLUMN reorder_level INT NOT NULL DEFAULT 5 AFTER stock_count',
    'SELECT ''product.reorder_level already exists; no schema change required'' AS message'
);

PREPARE inventory_patch_stmt FROM @reorder_level_sql;
EXECUTE inventory_patch_stmt;
DEALLOCATE PREPARE inventory_patch_stmt;
