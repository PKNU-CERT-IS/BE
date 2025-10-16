-- V35: Fix result_submit_status NOT NULL constraint

-- ===== 1. study 테이블 result_submit_status 컬럼에 NOT NULL 제약 추가 =====
-- 먼저 기존 NULL 값들을 'READY'로 업데이트
UPDATE study 
SET result_submit_status = 'READY' 
WHERE result_submit_status IS NULL;

ALTER TABLE study 
ALTER COLUMN result_submit_status SET NOT NULL;

-- ===== 2. project 테이블 result_submit_status 컬럼에 NOT NULL 제약 추가 =====
-- 먼저 기존 NULL 값들을 'READY'로 업데이트
UPDATE project 
SET result_submit_status = 'READY' 
WHERE result_submit_status IS NULL;

ALTER TABLE project 
ALTER COLUMN result_submit_status SET NOT NULL;
