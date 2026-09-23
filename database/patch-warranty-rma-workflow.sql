-- Function 4 - Warranty / RMA workflow upgrade
-- Target: EXISTING spareparts_mysimulation database
-- Safe-updates-friendly version

USE spareparts_mysimulation;

-- Temporarily disable MySQL Workbench Safe Updates for migration UPDATEs.
SET SQL_SAFE_UPDATES = 0;

SET @db_name = DATABASE();

-- -------------------------------------------------------------------------
-- 1. Keep claim workflow state separate from final resolution.
-- -------------------------------------------------------------------------
SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = @db_name
          AND TABLE_NAME = 'rma_claim'
          AND COLUMN_NAME = 'claim_status'
    ),
    'SELECT ''claim_status already exists''',
    'ALTER TABLE rma_claim
        ADD COLUMN claim_status
        ENUM(''pending'',''approved'',''rejected'',''closed'')
        NOT NULL DEFAULT ''pending''
        AFTER claim_date'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = @db_name
          AND TABLE_NAME = 'rma_claim'
          AND COLUMN_NAME = 'reviewed_by'
    ),
    'SELECT ''reviewed_by already exists''',
    'ALTER TABLE rma_claim
        ADD COLUMN reviewed_by INT NULL
        AFTER processed_by'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = @db_name
          AND TABLE_NAME = 'rma_claim'
          AND COLUMN_NAME = 'resolved_by'
    ),
    'SELECT ''resolved_by already exists''',
    'ALTER TABLE rma_claim
        ADD COLUMN resolved_by INT NULL
        AFTER reviewed_by'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = @db_name
          AND TABLE_NAME = 'rma_claim'
          AND COLUMN_NAME = 'replacement_serial_id'
    ),
    'SELECT ''replacement_serial_id already exists''',
    'ALTER TABLE rma_claim
        ADD COLUMN replacement_serial_id INT NULL
        AFTER resolved_by'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = @db_name
          AND TABLE_NAME = 'rma_claim'
          AND COLUMN_NAME = 'rejection_reason'
    ),
    'SELECT ''rejection_reason already exists''',
    'ALTER TABLE rma_claim
        ADD COLUMN rejection_reason VARCHAR(255) NULL
        AFTER condition_notes'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = @db_name
          AND TABLE_NAME = 'rma_claim'
          AND COLUMN_NAME = 'resolution_notes'
    ),
    'SELECT ''resolution_notes already exists''',
    'ALTER TABLE rma_claim
        ADD COLUMN resolution_notes VARCHAR(500) NULL
        AFTER rejection_reason'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add created_at as nullable first so old rows can be backfilled.
SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = @db_name
          AND TABLE_NAME = 'rma_claim'
          AND COLUMN_NAME = 'created_at'
    ),
    'SELECT ''created_at already exists''',
    'ALTER TABLE rma_claim
        ADD COLUMN created_at DATETIME NULL
        AFTER claim_date'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Backfill legacy RMA rows.
UPDATE rma_claim
SET created_at = TIMESTAMP(claim_date)
WHERE created_at IS NULL;

-- Make created_at mandatory after backfill.
ALTER TABLE rma_claim
    MODIFY COLUMN created_at
    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;

SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = @db_name
          AND TABLE_NAME = 'rma_claim'
          AND COLUMN_NAME = 'reviewed_at'
    ),
    'SELECT ''reviewed_at already exists''',
    'ALTER TABLE rma_claim
        ADD COLUMN reviewed_at DATETIME NULL
        AFTER created_at'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = @db_name
          AND TABLE_NAME = 'rma_claim'
          AND COLUMN_NAME = 'resolved_at'
    ),
    'SELECT ''resolved_at already exists''',
    'ALTER TABLE rma_claim
        ADD COLUMN resolved_at DATETIME NULL
        AFTER reviewed_at'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = @db_name
          AND TABLE_NAME = 'rma_claim'
          AND COLUMN_NAME = 'closed_at'
    ),
    'SELECT ''closed_at already exists''',
    'ALTER TABLE rma_claim
        ADD COLUMN closed_at DATETIME NULL
        AFTER resolved_at'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Map older already-resolved claims to closed.
UPDATE rma_claim
SET claim_status = CASE
    WHEN resolution = 'pending' THEN 'pending'
    ELSE 'closed'
END
WHERE claim_status = 'pending';

-- -------------------------------------------------------------------------
-- 2. Replacement serials need a real non-sellable state.
-- -------------------------------------------------------------------------
ALTER TABLE serial_number
    MODIFY COLUMN current_status
    ENUM('in_stock','sold','returned','defective','replacement')
    NOT NULL DEFAULT 'in_stock';

-- -------------------------------------------------------------------------
-- 3. Foreign keys for review/resolution actors and replacement serial.
-- -------------------------------------------------------------------------
SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.KEY_COLUMN_USAGE
        WHERE TABLE_SCHEMA = @db_name
          AND TABLE_NAME = 'rma_claim'
          AND COLUMN_NAME = 'reviewed_by'
          AND REFERENCED_TABLE_NAME = 'users'
    ),
    'SELECT ''fk_rma_reviewed_by already exists''',
    'ALTER TABLE rma_claim
        ADD CONSTRAINT fk_rma_reviewed_by
        FOREIGN KEY (reviewed_by)
        REFERENCES users(user_id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.KEY_COLUMN_USAGE
        WHERE TABLE_SCHEMA = @db_name
          AND TABLE_NAME = 'rma_claim'
          AND COLUMN_NAME = 'resolved_by'
          AND REFERENCED_TABLE_NAME = 'users'
    ),
    'SELECT ''fk_rma_resolved_by already exists''',
    'ALTER TABLE rma_claim
        ADD CONSTRAINT fk_rma_resolved_by
        FOREIGN KEY (resolved_by)
        REFERENCES users(user_id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.KEY_COLUMN_USAGE
        WHERE TABLE_SCHEMA = @db_name
          AND TABLE_NAME = 'rma_claim'
          AND COLUMN_NAME = 'replacement_serial_id'
          AND REFERENCED_TABLE_NAME = 'serial_number'
    ),
    'SELECT ''fk_rma_replacement_serial already exists''',
    'ALTER TABLE rma_claim
        ADD CONSTRAINT fk_rma_replacement_serial
        FOREIGN KEY (replacement_serial_id)
        REFERENCES serial_number(serial_id)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -------------------------------------------------------------------------
-- 4. Enforce claim_code uniqueness when legacy data allows it.
-- -------------------------------------------------------------------------
SET @has_index = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @db_name
      AND TABLE_NAME = 'rma_claim'
      AND COLUMN_NAME = 'claim_code'
      AND NON_UNIQUE = 0
);

SET @duplicate_codes = (
    SELECT COUNT(*)
    FROM (
        SELECT claim_code
        FROM rma_claim
        WHERE claim_code IS NOT NULL
        GROUP BY claim_code
        HAVING COUNT(*) > 1
    ) AS d
);

SET @sql = IF(
    @has_index > 0,
    'SELECT ''uk_rma_claim_code already exists''',
    IF(
        @duplicate_codes = 0,
        'ALTER TABLE rma_claim
            ADD UNIQUE INDEX uk_rma_claim_code (claim_code)',
        'SELECT ''WARNING: duplicate legacy claim_code values found; unique index not added'''
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -------------------------------------------------------------------------
-- 5. Verification
-- -------------------------------------------------------------------------
SELECT
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @db_name
  AND TABLE_NAME = 'rma_claim'
  AND COLUMN_NAME IN (
      'claim_status',
      'reviewed_by',
      'resolved_by',
      'replacement_serial_id',
      'rejection_reason',
      'resolution_notes',
      'created_at',
      'reviewed_at',
      'resolved_at',
      'closed_at'
  )
ORDER BY ORDINAL_POSITION;

SHOW COLUMNS
FROM serial_number
LIKE 'current_status';

-- Turn Safe Updates back on for normal Workbench use.
SET SQL_SAFE_UPDATES = 1;
