-- V34: Fix column length and default values issues

-- ===== 2. project 테이블 result_submit_status 컬럼 수정 =====
-- 기존 컬럼을 삭제하고 새로 생성 (DEFAULT 값 설정을 위해)
ALTER TABLE project DROP COLUMN IF EXISTS result_submit_status;

ALTER TABLE project 
ADD COLUMN result_submit_status VARCHAR(20) DEFAULT 'READY';

-- 기존 데이터가 있다면 READY로 업데이트 (새로 생성된 컬럼은 이미 DEFAULT 값이 적용됨)
-- UPDATE 문은 불필요하므로 제거

-- ===== 3. study 테이블 result_submit_status 컬럼 수정 =====
-- 기존 컬럼을 삭제하고 새로 생성 (DEFAULT 값 설정을 위해)
ALTER TABLE study DROP COLUMN IF EXISTS result_submit_status;

ALTER TABLE study 
ADD COLUMN result_submit_status VARCHAR(20) DEFAULT 'READY';

-- 기존 데이터가 있다면 READY로 업데이트 (새로 생성된 컬럼은 이미 DEFAULT 값이 적용됨)
-- UPDATE 문은 불필요하므로 제거

-- ===== 4. project 테이블 status 컬럼도 동일한 방식으로 수정 =====
-- 기존 컬럼을 삭제하고 새로 생성 (DEFAULT 값 설정을 위해)
ALTER TABLE project DROP COLUMN IF EXISTS status;

ALTER TABLE project 
ADD COLUMN status VARCHAR(20) DEFAULT 'READY' NOT NULL;

-- 기존 데이터가 있다면 READY로 업데이트 (새로 생성된 컬럼은 이미 DEFAULT 값이 적용됨)
-- UPDATE 문은 불필요하므로 제거

-- ===== 5. study 테이블 status 컬럼도 동일한 방식으로 수정 =====
-- 기존 컬럼을 삭제하고 새로 생성 (DEFAULT 값 설정을 위해)
ALTER TABLE study DROP COLUMN IF EXISTS status;

ALTER TABLE study 
ADD COLUMN status VARCHAR(20) DEFAULT 'READY' NOT NULL;

-- 기존 데이터가 있다면 READY로 업데이트 (새로 생성된 컬럼은 이미 DEFAULT 값이 적용됨)
-- UPDATE 문은 불필요하므로 제거
