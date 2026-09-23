-- Function 3 Stock Monitoring / Urgency upgrade for an EXISTING
-- spareparts_mysimulation database.
--
-- Run this once before starting the updated application. It widens the
-- stock_request status enum so stock arrival can mean "Ready to Notify"
-- without falsely claiming the customer has already been contacted.
USE spareparts_mysimulation;

ALTER TABLE stock_request
    MODIFY COLUMN status ENUM(
        'pending',
        'ready_to_notify',
        'notified',
        'fulfilled'
    ) NOT NULL DEFAULT 'pending';
