-- Add result submission fields to project and study

-- Project: add result_submitted_at, result_submit_status, result_attachments
ALTER TABLE project
    ADD COLUMN IF NOT EXISTS result_submitted_at TIMESTAMPTZ NULL,
    ADD COLUMN IF NOT EXISTS result_submit_status VARCHAR NOT NULL DEFAULT 'READY',
    ADD COLUMN IF NOT EXISTS result_attached_url VARCHAR NULL;

-- Study: add result_submitted_at, result_submit_status, result_attachments
ALTER TABLE study
    ADD COLUMN IF NOT EXISTS result_submitted_at TIMESTAMPTZ NULL,
    ADD COLUMN IF NOT EXISTS result_submit_status VARCHAR NOT NULL DEFAULT 'READY',
    ADD COLUMN IF NOT EXISTS result_attached_url VARCHAR NULL;

-- Optional indexes for admin review queries
CREATE INDEX IF NOT EXISTS idx_project_result_status ON project (result_submit_status);
CREATE INDEX IF NOT EXISTS idx_study_result_status ON study (result_submit_status);


