-- V27: Board content 필드 길이 제한 해제 (VARCHAR -> TEXT)
-- 게시글 내용이 100,000자까지 가능하도록 변경

-- board 테이블의 content 필드를 TEXT로 변경
ALTER TABLE board ALTER COLUMN content TYPE TEXT;

-- blog 테이블의 content 필드도 TEXT로 변경 (일관성 유지)
ALTER TABLE blog ALTER COLUMN content TYPE TEXT;

-- study 테이블의 content 필드도 TEXT로 변경 (일관성 유지)  
ALTER TABLE study ALTER COLUMN content TYPE TEXT;

-- project 테이블의 content 필드도 TEXT로 변경 (일관성 유지)
ALTER TABLE project ALTER COLUMN content TYPE TEXT;
