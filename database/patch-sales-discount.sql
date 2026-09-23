-- Function 2 - Sales / POS fixed per-unit discount support
-- Target: EXISTING spareparts_mysimulation database
-- Safe to run more than once. Run this before starting the updated application
-- because spring.jpa.hibernate.ddl-auto=validate expects discount_amount.

USE spareparts_mysimulation;

SET @old_safe_updates = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;
SET @db_name = DATABASE();
SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = @db_name
          AND TABLE_NAME = 'sale_item'
          AND COLUMN_NAME = 'discount_amount'
    ),
    'SELECT ''sale_item.discount_amount already exists''',
    'ALTER TABLE sale_item ADD COLUMN discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 AFTER price_at_sale'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE sale_item
SET discount_amount = 0.00
WHERE discount_amount IS NULL;

SET SQL_SAFE_UPDATES = @old_safe_updates;

SHOW COLUMNS FROM sale_item LIKE 'discount_amount';
