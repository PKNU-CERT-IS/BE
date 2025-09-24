-- V32: Change meeting link foreign keys from project_id/study_id to meeting_id
-- This migration changes the foreign key relationships for meeting links
-- to reference individual meetings instead of projects/studies

-- Step 1: Add new meeting_id columns
ALTER TABLE project_meeting_link ADD COLUMN meeting_id BIGINT;
ALTER TABLE study_meeting_link ADD COLUMN meeting_id BIGINT;

-- Step 2: Populate meeting_id columns with data from project_meeting and study_meeting
-- For project_meeting_link, we need to find the corresponding project_meeting.id
-- Since we can't directly map project_id to meeting_id without additional context,
-- we'll set meeting_id to NULL initially and require data migration
UPDATE project_meeting_link 
SET meeting_id = (
    SELECT pm.id 
    FROM project_meeting pm 
    WHERE pm.project_id = project_meeting_link.project_id 
    AND pm.deleted_at IS NULL
    LIMIT 1
);

UPDATE study_meeting_link 
SET meeting_id = (
    SELECT sm.id 
    FROM study_meeting sm 
    WHERE sm.study_id = study_meeting_link.study_id 
    AND sm.deleted_at IS NULL
    LIMIT 1
);

-- Step 3: Make meeting_id NOT NULL after data population
ALTER TABLE project_meeting_link ALTER COLUMN meeting_id SET NOT NULL;
ALTER TABLE study_meeting_link ALTER COLUMN meeting_id SET NOT NULL;

-- Step 4: Add foreign key constraints for meeting_id
ALTER TABLE project_meeting_link 
ADD CONSTRAINT fk_project_meeting_link_meeting 
FOREIGN KEY (meeting_id) REFERENCES project_meeting(id);

ALTER TABLE study_meeting_link 
ADD CONSTRAINT fk_study_meeting_link_meeting 
FOREIGN KEY (meeting_id) REFERENCES study_meeting(id);

-- Step 5: Drop old foreign key constraints
ALTER TABLE project_meeting_link DROP CONSTRAINT fk_project_meeting_link_project;
ALTER TABLE study_meeting_link DROP CONSTRAINT fk_study_meeting_link_study;

-- Step 6: Drop old columns
ALTER TABLE project_meeting_link DROP COLUMN project_id;
ALTER TABLE study_meeting_link DROP COLUMN study_id;
