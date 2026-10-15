-- Phase 2.2: allow the new PROCESSING status.
-- Needed only for databases created before this phase: Hibernate's ddl-auto=update never alters
-- existing CHECK constraints. Fresh databases get the correct constraint from the entity mapping.
ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_status_check;
ALTER TABLE payments ADD CONSTRAINT payments_status_check
    CHECK (status IN ('PENDING', 'PROCESSING', 'SUCCEEDED', 'FAILED'));
