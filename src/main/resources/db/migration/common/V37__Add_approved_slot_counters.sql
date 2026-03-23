ALTER TABLE project
    ADD COLUMN approved_slots_used INTEGER NOT NULL DEFAULT 0;

ALTER TABLE study
    ADD COLUMN approved_slots_used INTEGER NOT NULL DEFAULT 0;

UPDATE project p
SET approved_slots_used = approved.approved_count
FROM (
    SELECT project_id, COUNT(*) AS approved_count
    FROM project_participant
    WHERE deleted_at IS NULL
      AND status = 'APPROVED'
    GROUP BY project_id
) approved
WHERE p.id = approved.project_id;

UPDATE study s
SET approved_slots_used = approved.approved_count
FROM (
    SELECT study_id, COUNT(*) AS approved_count
    FROM study_participant
    WHERE deleted_at IS NULL
      AND status = 'APPROVED'
    GROUP BY study_id
) approved
WHERE s.id = approved.study_id;
