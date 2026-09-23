-- Function 6 - Reporting / Audit persistent review support
-- Target: EXISTING spareparts_mysimulation database
-- Safe to run after the Warranty / RMA patch.

USE spareparts_mysimulation;

CREATE TABLE IF NOT EXISTS audit_review(
    review_id INT AUTO_INCREMENT PRIMARY KEY,
    audit_log_id INT NOT NULL UNIQUE,
    reviewed_by INT NOT NULL,
    review_status ENUM('reviewed','dismissed','action_required') NOT NULL,
    review_note VARCHAR(500),
    reviewed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_review_log
        FOREIGN KEY (audit_log_id) REFERENCES audit_log(log_id),
    CONSTRAINT fk_audit_review_user
        FOREIGN KEY (reviewed_by) REFERENCES users(user_id)
);

-- Verification
SHOW COLUMNS FROM audit_review;
SHOW CREATE TABLE audit_review;
