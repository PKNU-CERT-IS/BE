-- Add demo_url column to project table
-- V28__Add_demo_url_to_project.sql

-- Add demo_url column to project table
ALTER TABLE project ADD COLUMN demo_url VARCHAR;

-- Add comment for the new column
COMMENT ON COLUMN project.demo_url IS '프로젝트 데모 URL';
