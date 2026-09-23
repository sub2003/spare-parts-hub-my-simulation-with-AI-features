-- OPTIONAL demo-only data for Supplier Portal subset testing.
-- Use only if you kept an existing database instead of recreating from simulation-seed.sql.
USE spareparts_mysimulation;

INSERT INTO supplier(supplier_code,name,contact,email,password_hash,active)
SELECT 'SUP004','Inactive Demo Supplier','0774445566','supplier4@demo.lk',
       '$2y$10$xBpTh63hXeF5bycwhPXqsO.HNQq/tw7gljGwRpjn2v1c9u.VRWF1O',FALSE
WHERE NOT EXISTS (SELECT 1 FROM supplier WHERE email='supplier4@demo.lk');
