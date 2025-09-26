-- V33: Add status fields to study and project tables
-- This migration adds status fields to both study and project tables
-- to support the progress status management system

-- Add status column to study table
ALTER TABLE study ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'READY';

-- Add status column to project table  
ALTER TABLE project ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'READY';

-- Update existing records to have appropriate status based on their current state
-- For studies: if started_at is in the past, set to INPROGRESS, otherwise READY
UPDATE study 
SET status = CASE 
    WHEN started_at <= NOW() THEN 'INPROGRESS'
    ELSE 'READY'
END
WHERE deleted_at IS NULL;

-- For projects: if started_at is in the past, set to INPROGRESS, otherwise READY
UPDATE project 
SET status = CASE 
    WHEN started_at <= NOW() THEN 'INPROGRESS'
    ELSE 'READY'
END
WHERE deleted_at IS NULL;

-- Add comments for documentation
COMMENT ON COLUMN study.status IS 'Study status: READY, APPROVED, INPROGRESS, COMPLETED, REJECTED';
COMMENT ON COLUMN project.status IS 'Project status: READY, APPROVED, INPROGRESS, COMPLETED, REJECTED';
