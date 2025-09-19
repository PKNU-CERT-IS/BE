-- V0__Drop_all_tables.sql
-- 애플리케이션 시작 시 모든 테이블을 강제 삭제

-- ✅ 1단계: 외래키 제약조건 일시 비활성화
SET session_replication_role = replica;

-- ✅ 2단계: 모든 테이블 강제 삭제 (의존성 순서 무시)
DROP TABLE IF EXISTS auth CASCADE;
DROP TABLE IF EXISTS blog_tag CASCADE;
DROP TABLE IF EXISTS blog_view CASCADE;
DROP TABLE IF EXISTS blog CASCADE;
DROP TABLE IF EXISTS board_attached CASCADE;
DROP TABLE IF EXISTS board_like CASCADE;
DROP TABLE IF EXISTS board_report CASCADE;
DROP TABLE IF EXISTS board_view CASCADE;
DROP TABLE IF EXISTS board CASCADE;
DROP TABLE IF EXISTS member_contact CASCADE;
DROP TABLE IF EXISTS member_penalty CASCADE;
DROP TABLE IF EXISTS member CASCADE;
DROP TABLE IF EXISTS project_attached CASCADE;
DROP TABLE IF EXISTS project_participant CASCADE;
DROP TABLE IF EXISTS project_tag CASCADE;
DROP TABLE IF EXISTS project CASCADE;
DROP TABLE IF EXISTS schedule_attached CASCADE;
DROP TABLE IF EXISTS schedule_status CASCADE;
DROP TABLE IF EXISTS schedule CASCADE;
DROP TABLE IF EXISTS study_attached CASCADE;
DROP TABLE IF EXISTS study_meeting CASCADE;
DROP TABLE IF EXISTS study_participant CASCADE;
DROP TABLE IF EXISTS study_tag CASCADE;
DROP TABLE IF EXISTS study CASCADE;

-- ✅ 3단계: 모든 시퀀스 삭제
DROP SEQUENCE IF EXISTS member_id_seq CASCADE;
DROP SEQUENCE IF EXISTS blog_id_seq CASCADE;
DROP SEQUENCE IF EXISTS blog_tag_id_seq CASCADE;
DROP SEQUENCE IF EXISTS blog_view_id_seq CASCADE;
DROP SEQUENCE IF EXISTS board_id_seq CASCADE;
DROP SEQUENCE IF EXISTS board_attached_id_seq CASCADE;
DROP SEQUENCE IF EXISTS board_like_id_seq CASCADE;
DROP SEQUENCE IF EXISTS board_report_id_seq CASCADE;
DROP SEQUENCE IF EXISTS board_view_id_seq CASCADE;
DROP SEQUENCE IF EXISTS study_id_seq CASCADE;
DROP SEQUENCE IF EXISTS study_attached_id_seq CASCADE;
DROP SEQUENCE IF EXISTS study_meeting_id_seq CASCADE;
DROP SEQUENCE IF EXISTS study_participant_id_seq CASCADE;
DROP SEQUENCE IF EXISTS study_tag_id_seq CASCADE;
DROP SEQUENCE IF EXISTS project_id_seq CASCADE;
DROP SEQUENCE IF EXISTS project_attached_id_seq CASCADE;
DROP SEQUENCE IF EXISTS project_participant_id_seq CASCADE;
DROP SEQUENCE IF EXISTS project_tag_id_seq CASCADE;
DROP SEQUENCE IF EXISTS schedule_id_seq CASCADE;
DROP SEQUENCE IF EXISTS schedule_attached_id_seq CASCADE;
DROP SEQUENCE IF EXISTS schedule_status_id_seq CASCADE;

-- ✅ 4단계: 외래키 제약조건 재활성화
SET session_replication_role = DEFAULT;

-- ✅ 완료 로그
SELECT 'All tables and sequences dropped successfully!' as status; 