-- Run this ONCE only if you are keeping an existing spareparts_mysimulation database.
-- If you recreate the DB from database/schema-simulation.sql, do not run this patch.
USE spareparts_mysimulation;

ALTER TABLE users
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE AFTER password_hash;
