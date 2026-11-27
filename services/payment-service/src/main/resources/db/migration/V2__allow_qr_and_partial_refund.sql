-- Databases created by Hibernate before QR payments and partial refunds existed still had check
-- constraints listing only the old values, so saving an ABA_PAYWAY_QR or PARTIALLY_REFUNDED payment
-- failed. (Hibernate's ddl-auto=update never changes a check constraint.) Re-create both.
-- Adding a value to PaymentMethod / PaymentStatus needs a migration like this one.
ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_method_check;
ALTER TABLE payments ADD CONSTRAINT payments_method_check
    CHECK (method IN ('ABA_PAYWAY', 'ABA_PAYWAY_QR', 'BAKONG_KHQR'));

ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_status_check;
ALTER TABLE payments ADD CONSTRAINT payments_status_check
    CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'EXPIRED', 'CANCELLED', 'PARTIALLY_REFUNDED', 'REFUNDED'));
