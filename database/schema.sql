CREATE DATABASE IF NOT EXISTS spare_parts_db;
USE spare_parts_db;

CREATE TABLE users(
                      user_id INT AUTO_INCREMENT PRIMARY KEY,
                      user_code VARCHAR(20),
                      name VARCHAR(50) NOT NULL,
                      role ENUM('warehouse_clerk','sales_exec','inventory_supervisor','operations_coordinator','admin') NOT NULL,
                      email VARCHAR(254) UNIQUE NOT NULL,
                      password_hash VARCHAR(60) NOT NULL
);

CREATE TABLE storage_location(
                                 location_id INT AUTO_INCREMENT PRIMARY KEY,
                                 location_code VARCHAR(20),
                                 aisle VARCHAR(30) NOT NULL,
                                 shelf VARCHAR(30) NOT NULL,
                                 bin VARCHAR(30) NOT NULL
);

-- password_hash added: Supplier is a primary actor with its own portal
-- login in UC-06, separate from staff auth (see SupplierSecurityConfig).
CREATE TABLE supplier(
                         supplier_id INT AUTO_INCREMENT PRIMARY KEY,
                         supplier_code VARCHAR(20),
                         name VARCHAR(50) NOT NULL,
                         contact VARCHAR(20) NOT NULL,
                         email VARCHAR(254) UNIQUE NOT NULL,
                         password_hash VARCHAR(60) NOT NULL
);

CREATE TABLE compatibility_rule(
                                   rule_id INT AUTO_INCREMENT PRIMARY KEY,
                                   spec_type VARCHAR(30) NOT NULL,
                                   value_a VARCHAR(30) NOT NULL,
                                   value_b VARCHAR(30) NOT NULL,
                                   reason VARCHAR(200) NOT NULL
);

CREATE TABLE partnership_request(
                                    request_id INT AUTO_INCREMENT PRIMARY KEY,
                                    company_name VARCHAR(100) NOT NULL,
                                    contact_person VARCHAR(50) NOT NULL,
                                    email VARCHAR(254) NOT NULL,
                                    catalog_file_path VARCHAR(255),
                                    status ENUM('pending','approved','rejected') NOT NULL DEFAULT 'pending',
                                    submitted_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE product(
                        product_id INT AUTO_INCREMENT PRIMARY KEY,
                        product_code VARCHAR(20) NOT NULL UNIQUE,
                        location_id INT,
                        name VARCHAR(50) NOT NULL,
                        category VARCHAR(30) NOT NULL,
                        brand VARCHAR(30) NOT NULL,
                        price DECIMAL(10,2) NOT NULL,
                        stock_count INT NOT NULL DEFAULT 0,
                        reorder_level INT NOT NULL DEFAULT 5,
                        urgency_score DECIMAL(6,2) DEFAULT 0,
                        warranty_period_months INT,
                        serial_tracked BOOLEAN NOT NULL DEFAULT FALSE,
                        FOREIGN KEY (location_id) REFERENCES storage_location(location_id)
);

CREATE TABLE product_spec(
                             product_spec_id INT AUTO_INCREMENT PRIMARY KEY,
                             product_id INT NOT NULL,
                             spec_type VARCHAR(30) NOT NULL,
                             spec_value VARCHAR(30) NOT NULL,
                             FOREIGN KEY (product_id) REFERENCES product(product_id)
);

CREATE TABLE sale(
                     sale_id INT AUTO_INCREMENT PRIMARY KEY,
                     sale_code VARCHAR(20),
                     sold_by INT NOT NULL,
                     sold_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                     amount DECIMAL(10,2) NOT NULL,
                     FOREIGN KEY (sold_by) REFERENCES users(user_id)
);

-- compatibility_override_reason added: UC-02 step 6a requires recording
-- why an authorized override was allowed when a conflict was detected.
-- discount_amount + discount_reason: UC-02 uses a transaction-only, fixed
-- per-unit LKR discount. price_at_sale stores the final unit price actually
-- charged; original unit price remains historically recoverable as
-- price_at_sale + discount_amount. The product catalog price is never edited.
CREATE TABLE sale_item(
                          sale_item_id INT AUTO_INCREMENT PRIMARY KEY,
                          sale_id INT NOT NULL,
                          product_id INT NOT NULL,
                          quantity INT NOT NULL,
                          price_at_sale DECIMAL(10,2) NOT NULL,
                          discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
                          compatibility_override_reason VARCHAR(255),
                          discount_reason VARCHAR(255),
                          FOREIGN KEY (sale_id) REFERENCES sale(sale_id),
                          FOREIGN KEY (product_id) REFERENCES product(product_id)
);

-- status widened + ticket_code added: UC-01 needs Completed / Partially
-- Completed / Exception outcomes and a human-readable ticket number, not
-- just a pending/fulfilled binary.
CREATE TABLE pick_ticket(
                            ticket_id INT AUTO_INCREMENT PRIMARY KEY,
                            ticket_code VARCHAR(20),
                            sale_id INT NOT NULL,
                            fulfilled_by INT,
                            status ENUM('pending','completed','partially_completed','exception') NOT NULL DEFAULT 'pending',
                            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                            fulfilled_at DATETIME,
                            FOREIGN KEY (sale_id) REFERENCES sale(sale_id),
                            FOREIGN KEY (fulfilled_by) REFERENCES users(user_id)
);

-- picked_quantity added: UC-01 step 7a requires recording the actual
-- quantity picked, which may differ from what was requested.
CREATE TABLE pick_ticket_item(
                                 pick_ticket_item_id INT AUTO_INCREMENT PRIMARY KEY,
                                 ticket_id INT NOT NULL,
                                 product_id INT NOT NULL,
                                 quantity INT NOT NULL,
                                 picked_quantity INT,
                                 FOREIGN KEY (ticket_id) REFERENCES pick_ticket(ticket_id),
                                 FOREIGN KEY (product_id) REFERENCES product(product_id)
);

CREATE TABLE stock_request(
                              request_id INT AUTO_INCREMENT PRIMARY KEY,
                              product_id INT NOT NULL,
                              logged_by INT NOT NULL,
                              customer_name VARCHAR(50) NOT NULL,
                              customer_email VARCHAR(254),
                              customer_phonenum VARCHAR(20),
                              status ENUM('pending','ready_to_notify','notified','fulfilled') NOT NULL DEFAULT 'pending',
                              requested_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                              notified_at DATETIME,
                              FOREIGN KEY (product_id) REFERENCES product(product_id),
                              FOREIGN KEY (logged_by) REFERENCES users(user_id)
);

CREATE TABLE supplier_product(
                                 supplier_product_id INT AUTO_INCREMENT PRIMARY KEY,
                                 supplier_id INT NOT NULL,
                                 product_id INT NOT NULL,
                                 price DECIMAL(10,2) NOT NULL,
                                 moq INT NOT NULL,
                                 lead_time_days INT NOT NULL,
                                 FOREIGN KEY (supplier_id) REFERENCES supplier(supplier_id),
                                 FOREIGN KEY (product_id) REFERENCES product(product_id)
);

-- status widened: UC-05 step 11 requires "Partially Received" as a real,
-- distinct state (a shipment can arrive short/incomplete).
CREATE TABLE purchase_order(
                               po_id INT AUTO_INCREMENT PRIMARY KEY,
                               po_code VARCHAR(20),
                               supplier_id INT NOT NULL,
                               created_by INT NOT NULL,
                               status ENUM('pending','shipped','partially_received','received') NOT NULL DEFAULT 'pending',
                               created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                               received_at DATETIME,
                               FOREIGN KEY (supplier_id) REFERENCES supplier(supplier_id),
                               FOREIGN KEY (created_by) REFERENCES users(user_id)
);

CREATE TABLE purchase_order_item(
                                    po_item_id INT AUTO_INCREMENT PRIMARY KEY,
                                    po_id INT NOT NULL,
                                    product_id INT NOT NULL,
                                    quantity_ordered INT NOT NULL,
                                    price_agreed DECIMAL(10,2) NOT NULL,
                                    FOREIGN KEY (po_id) REFERENCES purchase_order(po_id),
                                    FOREIGN KEY (product_id) REFERENCES product(product_id)
);

-- po_item_id added: UC-05 step 10 requires tracing a received serial
-- number back to the specific shipment/PO it arrived on. Moved below
-- purchase_order_item (was originally defined earlier in the file) so
-- this foreign key can be declared inline instead of via a later ALTER.
CREATE TABLE serial_number(
                              serial_id INT AUTO_INCREMENT PRIMARY KEY,
                              product_id INT NOT NULL,
                              po_item_id INT,
                              sale_id INT,
                              serial_value VARCHAR(50) NOT NULL UNIQUE,
                              current_status ENUM('in_stock','sold','returned','defective','replacement') NOT NULL DEFAULT 'in_stock',
                              received_date DATE NOT NULL,
                              FOREIGN KEY (product_id) REFERENCES product(product_id),
                              FOREIGN KEY (po_item_id) REFERENCES purchase_order_item(po_item_id),
                              FOREIGN KEY (sale_id) REFERENCES sale(sale_id)
);

-- Function 4 keeps claim workflow state separate from final resolution.
-- Review/resolution actors and timestamps provide traceability, while
-- replacement_serial_id prevents a physical replacement from being lost in
-- free-text notes. claim_code is unique and generated from the claim ID.
CREATE TABLE rma_claim(
                          claim_id INT AUTO_INCREMENT PRIMARY KEY,
                          claim_code VARCHAR(20) UNIQUE,
                          serial_id INT NOT NULL,
                          processed_by INT NOT NULL,
                          reviewed_by INT,
                          resolved_by INT,
                          replacement_serial_id INT,
                          fault_description VARCHAR(255),
                          condition_notes VARCHAR(255),
                          rejection_reason VARCHAR(255),
                          resolution_notes VARCHAR(500),
                          claim_date DATE NOT NULL DEFAULT (CURRENT_DATE),
                          created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          reviewed_at DATETIME,
                          resolved_at DATETIME,
                          closed_at DATETIME,
                          claim_status ENUM('pending','approved','rejected','closed') NOT NULL DEFAULT 'pending',
                          resolution ENUM('sent_to_manufacturer','refunded','replaced','pending') NOT NULL DEFAULT 'pending',
                          FOREIGN KEY (serial_id) REFERENCES serial_number(serial_id),
                          FOREIGN KEY (processed_by) REFERENCES users(user_id),
                          FOREIGN KEY (reviewed_by) REFERENCES users(user_id),
                          FOREIGN KEY (resolved_by) REFERENCES users(user_id),
                          FOREIGN KEY (replacement_serial_id) REFERENCES serial_number(serial_id)
);

-- New table: UC-03 postcondition 3 requires the Inventory Supervisor's
-- approve/modify/reject decision (with reason) to be stored, separate
-- from the eventual purchase order Function 5 creates from approved
-- items.
CREATE TABLE restock_suggestion(
                                   suggestion_id INT AUTO_INCREMENT PRIMARY KEY,
                                   product_id INT NOT NULL,
                                   suggested_quantity INT NOT NULL,
                                   status ENUM('pending','approved','modified','rejected') NOT NULL DEFAULT 'pending',
                                   reason VARCHAR(255),
                                   reviewed_by INT,
                                   created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                   reviewed_at DATETIME,
                                   FOREIGN KEY (product_id) REFERENCES product(product_id),
                                   FOREIGN KEY (reviewed_by) REFERENCES users(user_id)
);

CREATE TABLE audit_log(
                          log_id INT AUTO_INCREMENT PRIMARY KEY,
                          user_id INT NOT NULL,
                          action_type VARCHAR(30) NOT NULL,
                          table_name VARCHAR(50) NOT NULL,
                          record_id INT NOT NULL,
                          old_value JSON,
                          new_value JSON,
                          logged_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                          FOREIGN KEY (user_id) REFERENCES users(user_id)
);



-- Function 6 keeps administrative review separate from the immutable audit event.
CREATE TABLE audit_review(
                             review_id INT AUTO_INCREMENT PRIMARY KEY,
                             audit_log_id INT NOT NULL UNIQUE,
                             reviewed_by INT NOT NULL,
                             review_status ENUM('reviewed','dismissed','action_required') NOT NULL,
                             review_note VARCHAR(500),
                             reviewed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             CONSTRAINT fk_audit_review_log FOREIGN KEY (audit_log_id) REFERENCES audit_log(log_id),
                             CONSTRAINT fk_audit_review_user FOREIGN KEY (reviewed_by) REFERENCES users(user_id)
);

CREATE TABLE restock_offer(
                              offer_id INT AUTO_INCREMENT PRIMARY KEY,
                              supplier_id INT NOT NULL,
                              product_id INT NOT NULL,
                              price DECIMAL(10,2) NOT NULL,
                              quantity INT NOT NULL,
                              submitted_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                              FOREIGN KEY (supplier_id) REFERENCES supplier(supplier_id),
                              FOREIGN KEY (product_id) REFERENCES product(product_id)
);