-- V36: Ensure result_submit_status has DEFAULT 'READY' and NOT NULL on project/study

-- PROJECT: set default
ALTER TABLE project ALTER COLUMN result_submit_status SET DEFAULT 'READY';

-- STUDY: set default
ALTER TABLE study ALTER COLUMN result_submit_status SET DEFAULT 'READY';


