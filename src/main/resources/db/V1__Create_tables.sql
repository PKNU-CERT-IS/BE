-- Schema for CERT-IS Study Platform
-- 순수 CREATE 전용 (DROP은 V0__Drop_all_tables.sql에서 처리)

-- ✅ 스키마 생성 시작 로그
SELECT 'Starting schema creation...' as status;

-- Create base tables first (no foreign key dependencies)

CREATE TABLE member (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR NOT NULL,
    student_number VARCHAR NOT NULL,
    profile_image VARCHAR DEFAULT 'http://example.com',
    grade VARCHAR NOT NULL DEFAULT '',
    role VARCHAR NOT NULL DEFAULT '',
    skills TEXT[],CREATE TABLE member (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR NOT NULL,
    student_number VARCHAR NOT NULL,
    profile_image VARCHAR DEFAULT 'http://example.com',
    grade VARCHAR NOT NULL DEFAULT '',
    role VARCHAR NOT NULL DEFAULT '',
    skills TEXT[],
    major VARCHAR,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    birthday TIMESTAMPTZ NOT NULL,
    gender VARCHAR NOT NULL,
    description VARCHAR
);

-- Create dependent tables for member

CREATE TABLE member_contact (
                                member_id BIGINT NOT NULL PRIMARY KEY,
                                email VARCHAR NOT NULL,
                                github_url VARCHAR,
                                linkedin_url VARCHAR,
                                phone_number VARCHAR NOT NULL,
                                updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                CONSTRAINT fk_member_contact_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE member_penalty (
                                member_id BIGINT NOT NULL PRIMARY KEY,
                                penalty_point INTEGER NOT NULL DEFAULT 0,
                                penaltied_at TIMESTAMPTZ NOT NULL,
                                updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                reason VARCHAR NOT NULL,
                                CONSTRAINT fk_member_penalty_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE auth (
                      member_id BIGINT NOT NULL PRIMARY KEY,
                      account_number VARCHAR NOT NULL UNIQUE,
                      password VARCHAR NOT NULL,
                      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                      deleted_at TIMESTAMPTZ,
                      CONSTRAINT fk_auth_member FOREIGN KEY (member_id) REFERENCES member(id)
);

-- Create main entity tables

CREATE TABLE study (
                       id BIGSERIAL PRIMARY KEY,
                       member_id BIGINT NOT NULL,
                       title VARCHAR NOT NULL,
                       content VARCHAR NOT NULL,
                       type VARCHAR NOT NULL,
                       skills TEXT[] NOT NULL,
                       category VARCHAR NOT NULL,
                       subcategory VARCHAR NOT NULL,
                       created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                       deleted_at TIMESTAMPTZ,
                       started_at TIMESTAMPTZ NOT NULL,
                       ended_at TIMESTAMPTZ NOT NULL,
                       max_participants_number INTEGER NOT NULL DEFAULT 5,
                       description VARCHAR NOT NULL,
                       github_url VARCHAR,
                       CONSTRAINT fk_study_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE project (
                         id BIGSERIAL PRIMARY KEY,
                         member_id BIGINT NOT NULL,
                         title VARCHAR NOT NULL,
                         content VARCHAR NOT NULL,
                         skills TEXT[] NOT NULL,
                         difficulty VARCHAR NOT NULL,
                         created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                         updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                         deleted_at TIMESTAMPTZ,
                         category VARCHAR NOT NULL,
                         description VARCHAR NOT NULL,
                         started_at TIMESTAMPTZ NOT NULL,
                         ended_at TIMESTAMPTZ NOT NULL,
                         max_participants_number INTEGER NOT NULL,
                         CONSTRAINT fk_project_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE schedule (
                          id BIGSERIAL PRIMARY KEY,
                          member_id BIGINT NOT NULL,
                          type VARCHAR NOT NULL,
                          title VARCHAR NOT NULL,
                          description VARCHAR NOT NULL,
                          content VARCHAR NOT NULL,
                          created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                          updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                          deleted_at TIMESTAMPTZ,
                          started_at TIMESTAMPTZ NOT NULL,
                          ended_at TIMESTAMPTZ NOT NULL,
                          CONSTRAINT fk_schedule_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE blog (
                      id BIGSERIAL PRIMARY KEY,
                      member_id BIGINT NOT NULL,
                      title VARCHAR NOT NULL,
                      content VARCHAR NOT NULL,
                      category VARCHAR NOT NULL,
                      is_published BOOLEAN NOT NULL DEFAULT FALSE,
                      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                      deleted_at TIMESTAMPTZ,
                      CONSTRAINT fk_blog_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE board (
                       id BIGSERIAL PRIMARY KEY,
                       member_id BIGINT NOT NULL,
                       title VARCHAR NOT NULL,
                       content VARCHAR NOT NULL,
                       category VARCHAR NOT NULL,
                       created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                       deleted_at TIMESTAMPTZ,
                       CONSTRAINT fk_board_member FOREIGN KEY (member_id) REFERENCES member(id)
);


CREATE TABLE blog_view (
                           id BIGSERIAL PRIMARY KEY,
                           blog_id BIGINT NOT NULL,
                           view_number INTEGER NOT NULL DEFAULT 0,
                           updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                           CONSTRAINT fk_blog_view_blog FOREIGN KEY (blog_id) REFERENCES blog(id)
);

-- Create dependent tables for board

CREATE TABLE board_attached (
                                id BIGSERIAL PRIMARY KEY,
                                board_id BIGINT NOT NULL,
                                member_id BIGINT NOT NULL,
                                attached_url VARCHAR NOT NULL,
                                name VARCHAR NOT NULL,
                                type VARCHAR NOT NULL,
                                size VARCHAR NOT NULL,
                                created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                deleted_at TIMESTAMPTZ,
                                CONSTRAINT fk_board_attached_board FOREIGN KEY (board_id) REFERENCES board(id),
                                CONSTRAINT fk_board_attached_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE board_like (
                            id BIGSERIAL PRIMARY KEY,
                            board_id BIGINT NOT NULL,
                            member_id BIGINT NOT NULL,
                            created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                            updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                            deleted_at TIMESTAMPTZ,
                            CONSTRAINT fk_board_like_board FOREIGN KEY (board_id) REFERENCES board(id),
                            CONSTRAINT fk_board_like_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE board_report (
                              id BIGSERIAL PRIMARY KEY,
                              board_id BIGINT NOT NULL,
                              member_id BIGINT NOT NULL,
                              content VARCHAR NOT NULL,
                              created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                              deleted_at TIMESTAMPTZ,
                              CONSTRAINT fk_board_report_board FOREIGN KEY (board_id) REFERENCES board(id),
                              CONSTRAINT fk_board_report_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE board_view (
                            id BIGSERIAL PRIMARY KEY,
                            board_id BIGINT NOT NULL,
                            view_number INTEGER NOT NULL DEFAULT 0,
                            updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                            CONSTRAINT fk_board_view_board FOREIGN KEY (board_id) REFERENCES board(id)
);

-- Create dependent tables for project

CREATE TABLE project_attached (
                                  id BIGSERIAL PRIMARY KEY,
                                  project_id BIGINT NOT NULL,
                                  member_id BIGINT NOT NULL,
                                  attached_url VARCHAR NOT NULL,
                                  name VARCHAR NOT NULL,
                                  type VARCHAR NOT NULL,
                                  size VARCHAR NOT NULL,
                                  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                  deleted_at TIMESTAMPTZ,
                                  CONSTRAINT fk_project_attached_project FOREIGN KEY (project_id) REFERENCES project(id),
                                  CONSTRAINT fk_project_attached_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE project_participant (
                                     id BIGSERIAL PRIMARY KEY,
                                     project_id BIGINT NOT NULL,
                                     member_id BIGINT NOT NULL,
                                     created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                     updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                     deleted_at TIMESTAMPTZ,
                                     CONSTRAINT fk_project_participant_project FOREIGN KEY (project_id) REFERENCES project(id),
                                     CONSTRAINT fk_project_participant_member FOREIGN KEY (member_id) REFERENCES member(id)
);


CREATE TABLE project_meeting (
                                 id BIGSERIAL PRIMARY KEY,
                                 project_id BIGINT NOT NULL,
                                 member_id BIGINT NOT NULL,
                                 title VARCHAR NOT NULL,
                                 content VARCHAR NOT NULL,
                                 participants TEXT[] NOT NULL,
                                 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                 updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                 deleted_at TIMESTAMPTZ,
                                 CONSTRAINT fk_project_meeting_project FOREIGN KEY (project_id) REFERENCES project(id),
                                 CONSTRAINT fk_project_meeting_member FOREIGN KEY (member_id) REFERENCES member(id)
);

-- Create dependent tables for schedule

CREATE TABLE schedule_attached (
                                   id BIGSERIAL PRIMARY KEY,
                                   schedule_id BIGINT NOT NULL,
                                   member_id BIGINT NOT NULL,
                                   attached_url VARCHAR NOT NULL,
                                   name VARCHAR NOT NULL,
                                   type VARCHAR NOT NULL,
                                   size VARCHAR NOT NULL,
                                   created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                   updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                   deleted_at TIMESTAMPTZ,
                                   CONSTRAINT fk_schedule_attached_schedule FOREIGN KEY (schedule_id) REFERENCES schedule(id),
                                   CONSTRAINT fk_schedule_attached_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE schedule_status (
                                 id BIGSERIAL PRIMARY KEY,
                                 schedule_id BIGINT NOT NULL,
                                 status VARCHAR NOT NULL DEFAULT 'PENDING',
                                 updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                 CONSTRAINT fk_schedule_status_schedule FOREIGN KEY (schedule_id) REFERENCES schedule(id)
);

-- Create dependent tables for study

CREATE TABLE study_attached (
                                id BIGSERIAL PRIMARY KEY,
                                study_id BIGINT NOT NULL,
                                member_id BIGINT NOT NULL,
                                attached_url VARCHAR NOT NULL,
                                name VARCHAR NOT NULL,
                                type VARCHAR NOT NULL,
                                size VARCHAR NOT NULL,
                                created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                deleted_at TIMESTAMPTZ,
                                CONSTRAINT fk_study_attached_study FOREIGN KEY (study_id) REFERENCES study(id),
                                CONSTRAINT fk_study_attached_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE study_meeting (
                               id BIGSERIAL PRIMARY KEY,
                               study_id BIGINT NOT NULL,
                               member_id BIGINT NOT NULL,
                               title VARCHAR NOT NULL,
                               content VARCHAR NOT NULL,
                               participants TEXT[] NOT NULL,
                               created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                               updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                               deleted_at TIMESTAMPTZ,
                               CONSTRAINT fk_study_meeting_study FOREIGN KEY (study_id) REFERENCES study(id),
                               CONSTRAINT fk_study_meeting_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE study_participant (
                                   id BIGSERIAL PRIMARY KEY,
                                   study_id BIGINT NOT NULL,
                                   member_id BIGINT NOT NULL,
                                   created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                   updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                   deleted_at TIMESTAMPTZ,
                                   CONSTRAINT fk_study_participant_study FOREIGN KEY (study_id) REFERENCES study(id),
                                   CONSTRAINT fk_study_participant_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE study_meeting_link (
                                    id BIGSERIAL PRIMARY KEY,
                                    study_id BIGINT NOT NULL,
                                    member_id BIGINT NOT NULL,
                                    name VARCHAR NOT NULL,
                                    attached_url VARCHAR NOT NULL,
                                    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                    deleted_at TIMESTAMPTZ,
                                    CONSTRAINT fk_study_meeting_link_study FOREIGN KEY (study_id) REFERENCES study(id),
                                    CONSTRAINT fk_study_meeting_link_member FOREIGN KEY (member_id) REFERENCES member(id)
);-- Schema for CERT-IS Study Platform
-- 순수 CREATE 전용 (DROP은 V0__Drop_all_tables.sql에서 처리)

-- ✅ 스키마 생성 시작 로그
SELECT 'Starting schema creation...' as status;

-- Create base tables first (no foreign key dependencies)

CREATE TABLE member (
                        id BIGSERIAL PRIMARY KEY,
                        name VARCHAR NOT NULL,
                        student_number VARCHAR NOT NULL,
                        profile_image VARCHAR DEFAULT 'http://example.com',
                        grade VARCHAR NOT NULL DEFAULT '',
                        role VARCHAR NOT NULL DEFAULT '',
                        skills TEXT[],CREATE TABLE member (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR NOT NULL,
    student_number VARCHAR NOT NULL,
    profile_image VARCHAR DEFAULT 'http://example.com',
    grade VARCHAR NOT NULL DEFAULT '',
    role VARCHAR NOT NULL DEFAULT '',
    skills TEXT[],
    major VARCHAR,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    birthday TIMESTAMPTZ NOT NULL,
    gender VARCHAR NOT NULL,
    description VARCHAR
);

-- Create dependent tables for member

CREATE TABLE member_contact (
                                member_id BIGINT NOT NULL PRIMARY KEY,
                                email VARCHAR NOT NULL,
                                github_url VARCHAR,
                                linkedin_url VARCHAR,
                                phone_number VARCHAR NOT NULL,
                                updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                CONSTRAINT fk_member_contact_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE member_penalty (
                                member_id BIGINT NOT NULL PRIMARY KEY,
                                penalty_point INTEGER NOT NULL DEFAULT 0,
                                penaltied_at TIMESTAMPTZ NOT NULL,
                                updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                reason VARCHAR NOT NULL,
                                CONSTRAINT fk_member_penalty_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE auth (
                      member_id BIGINT NOT NULL PRIMARY KEY,
                      account_number VARCHAR NOT NULL UNIQUE,
                      password VARCHAR NOT NULL,
                      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                      deleted_at TIMESTAMPTZ,
                      CONSTRAINT fk_auth_member FOREIGN KEY (member_id) REFERENCES member(id)
);

-- Create main entity tables

CREATE TABLE study (
                       id BIGSERIAL PRIMARY KEY,
                       member_id BIGINT NOT NULL,
                       title VARCHAR NOT NULL,
                       content VARCHAR NOT NULL,
                       type VARCHAR NOT NULL,
                       skills TEXT[] NOT NULL,
                       category VARCHAR NOT NULL,
                       subcategory VARCHAR NOT NULL,
                       created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                       deleted_at TIMESTAMPTZ,
                       started_at TIMESTAMPTZ NOT NULL,
                       ended_at TIMESTAMPTZ NOT NULL,
                       max_participants_number INTEGER NOT NULL DEFAULT 5,
                       description VARCHAR NOT NULL,
                       github_url VARCHAR,
                       CONSTRAINT fk_study_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE project (
                         id BIGSERIAL PRIMARY KEY,
                         member_id BIGINT NOT NULL,
                         title VARCHAR NOT NULL,
                         content VARCHAR NOT NULL,
                         skills TEXT[] NOT NULL,
                         difficulty VARCHAR NOT NULL,
                         created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                         updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                         deleted_at TIMESTAMPTZ,
                         category VARCHAR NOT NULL,
                         description VARCHAR NOT NULL,
                         started_at TIMESTAMPTZ NOT NULL,
                         ended_at TIMESTAMPTZ NOT NULL,
                         max_participants_number INTEGER NOT NULL,
                         CONSTRAINT fk_project_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE schedule (
                          id BIGSERIAL PRIMARY KEY,
                          member_id BIGINT NOT NULL,
                          type VARCHAR NOT NULL,
                          title VARCHAR NOT NULL,
                          description VARCHAR NOT NULL,
                          content VARCHAR NOT NULL,
                          created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                          updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                          deleted_at TIMESTAMPTZ,
                          started_at TIMESTAMPTZ NOT NULL,
                          ended_at TIMESTAMPTZ NOT NULL,
                          CONSTRAINT fk_schedule_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE blog (
                      id BIGSERIAL PRIMARY KEY,
                      member_id BIGINT NOT NULL,
                      title VARCHAR NOT NULL,
                      content VARCHAR NOT NULL,
                      category VARCHAR NOT NULL,
                      is_published BOOLEAN NOT NULL DEFAULT FALSE,
                      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                      deleted_at TIMESTAMPTZ,
                      CONSTRAINT fk_blog_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE board (
                       id BIGSERIAL PRIMARY KEY,
                       member_id BIGINT NOT NULL,
                       title VARCHAR NOT NULL,
                       content VARCHAR NOT NULL,
                       category VARCHAR NOT NULL,
                       created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                       deleted_at TIMESTAMPTZ,
                       CONSTRAINT fk_board_member FOREIGN KEY (member_id) REFERENCES member(id)
);


CREATE TABLE blog_view (
                           id BIGSERIAL PRIMARY KEY,
                           blog_id BIGINT NOT NULL,
                           view_number INTEGER NOT NULL DEFAULT 0,
                           updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                           CONSTRAINT fk_blog_view_blog FOREIGN KEY (blog_id) REFERENCES blog(id)
);

-- Create dependent tables for board

CREATE TABLE board_attached (
                                id BIGSERIAL PRIMARY KEY,
                                board_id BIGINT NOT NULL,
                                member_id BIGINT NOT NULL,
                                attached_url VARCHAR NOT NULL,
                                name VARCHAR NOT NULL,
                                type VARCHAR NOT NULL,
                                size VARCHAR NOT NULL,
                                created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                deleted_at TIMESTAMPTZ,
                                CONSTRAINT fk_board_attached_board FOREIGN KEY (board_id) REFERENCES board(id),
                                CONSTRAINT fk_board_attached_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE board_like (
                            id BIGSERIAL PRIMARY KEY,
                            board_id BIGINT NOT NULL,
                            member_id BIGINT NOT NULL,
                            created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                            updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                            deleted_at TIMESTAMPTZ,
                            CONSTRAINT fk_board_like_board FOREIGN KEY (board_id) REFERENCES board(id),
                            CONSTRAINT fk_board_like_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE board_report (
                              id BIGSERIAL PRIMARY KEY,
                              board_id BIGINT NOT NULL,
                              member_id BIGINT NOT NULL,
                              content VARCHAR NOT NULL,
                              created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                              deleted_at TIMESTAMPTZ,
                              CONSTRAINT fk_board_report_board FOREIGN KEY (board_id) REFERENCES board(id),
                              CONSTRAINT fk_board_report_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE board_view (
                            id BIGSERIAL PRIMARY KEY,
                            board_id BIGINT NOT NULL,
                            view_number INTEGER NOT NULL DEFAULT 0,
                            updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                            CONSTRAINT fk_board_view_board FOREIGN KEY (board_id) REFERENCES board(id)
);

-- Create dependent tables for project

CREATE TABLE project_attached (
                                  id BIGSERIAL PRIMARY KEY,
                                  project_id BIGINT NOT NULL,
                                  member_id BIGINT NOT NULL,
                                  attached_url VARCHAR NOT NULL,
                                  name VARCHAR NOT NULL,
                                  type VARCHAR NOT NULL,
                                  size VARCHAR NOT NULL,
                                  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                  deleted_at TIMESTAMPTZ,
                                  CONSTRAINT fk_project_attached_project FOREIGN KEY (project_id) REFERENCES project(id),
                                  CONSTRAINT fk_project_attached_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE project_participant (
                                     id BIGSERIAL PRIMARY KEY,
                                     project_id BIGINT NOT NULL,
                                     member_id BIGINT NOT NULL,
                                     created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                     updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                     deleted_at TIMESTAMPTZ,
                                     CONSTRAINT fk_project_participant_project FOREIGN KEY (project_id) REFERENCES project(id),
                                     CONSTRAINT fk_project_participant_member FOREIGN KEY (member_id) REFERENCES member(id)
);


CREATE TABLE project_meeting (
                                 id BIGSERIAL PRIMARY KEY,
                                 project_id BIGINT NOT NULL,
                                 member_id BIGINT NOT NULL,
                                 title VARCHAR NOT NULL,
                                 content VARCHAR NOT NULL,
                                 participants TEXT[] NOT NULL,
                                 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                 updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                 deleted_at TIMESTAMPTZ,
                                 CONSTRAINT fk_project_meeting_project FOREIGN KEY (project_id) REFERENCES project(id),
                                 CONSTRAINT fk_project_meeting_member FOREIGN KEY (member_id) REFERENCES member(id)
);

-- Create dependent tables for schedule

CREATE TABLE schedule_attached (
                                   id BIGSERIAL PRIMARY KEY,
                                   schedule_id BIGINT NOT NULL,
                                   member_id BIGINT NOT NULL,
                                   attached_url VARCHAR NOT NULL,
                                   name VARCHAR NOT NULL,
                                   type VARCHAR NOT NULL,
                                   size VARCHAR NOT NULL,
                                   created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                   updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                   deleted_at TIMESTAMPTZ,
                                   CONSTRAINT fk_schedule_attached_schedule FOREIGN KEY (schedule_id) REFERENCES schedule(id),
                                   CONSTRAINT fk_schedule_attached_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE schedule_status (
                                 id BIGSERIAL PRIMARY KEY,
                                 schedule_id BIGINT NOT NULL,
                                 status VARCHAR NOT NULL DEFAULT 'PENDING',
                                 updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                 CONSTRAINT fk_schedule_status_schedule FOREIGN KEY (schedule_id) REFERENCES schedule(id)
);

-- Create dependent tables for study

CREATE TABLE study_attached (
                                id BIGSERIAL PRIMARY KEY,
                                study_id BIGINT NOT NULL,
                                member_id BIGINT NOT NULL,
                                attached_url VARCHAR NOT NULL,
                                name VARCHAR NOT NULL,
                                type VARCHAR NOT NULL,
                                size VARCHAR NOT NULL,
                                created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                deleted_at TIMESTAMPTZ,
                                CONSTRAINT fk_study_attached_study FOREIGN KEY (study_id) REFERENCES study(id),
                                CONSTRAINT fk_study_attached_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE study_meeting (
                               id BIGSERIAL PRIMARY KEY,
                               study_id BIGINT NOT NULL,
                               member_id BIGINT NOT NULL,
                               title VARCHAR NOT NULL,
                               content VARCHAR NOT NULL,
                               participants TEXT[] NOT NULL,
                               created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                               updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                               deleted_at TIMESTAMPTZ,
                               CONSTRAINT fk_study_meeting_study FOREIGN KEY (study_id) REFERENCES study(id),
                               CONSTRAINT fk_study_meeting_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE study_participant (
                                   id BIGSERIAL PRIMARY KEY,
                                   study_id BIGINT NOT NULL,
                                   member_id BIGINT NOT NULL,
                                   created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                   updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                   deleted_at TIMESTAMPTZ,
                                   CONSTRAINT fk_study_participant_study FOREIGN KEY (study_id) REFERENCES study(id),
                                   CONSTRAINT fk_study_participant_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE study_meeting_link (
                                    id BIGSERIAL PRIMARY KEY,
                                    study_id BIGINT NOT NULL,
                                    member_id BIGINT NOT NULL,
                                    name VARCHAR NOT NULL,
                                    attached_url VARCHAR NOT NULL,
                                    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                    deleted_at TIMESTAMPTZ,
                                    CONSTRAINT fk_study_meeting_link_study FOREIGN KEY (study_id) REFERENCES study(id),
                                    CONSTRAINT fk_study_meeting_link_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE project_meeting_link (
                                      id BIGSERIAL PRIMARY KEY,
                                      project_id BIGINT NOT NULL,
                                      member_id BIGINT NOT NULL,
                                      name VARCHAR NOT NULL,
                                      attached_url VARCHAR NOT NULL,
                                      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                      deleted_at TIMESTAMPTZ,
                                      CONSTRAINT fk_project_meeting_link_project FOREIGN KEY (project_id) REFERENCES project(id),
                                      CONSTRAINT fk_project_meeting_link_member FOREIGN KEY (member_id) REFERENCES member(id)
);

major VARCHAR,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    birthday TIMESTAMPTZ NOT NULL,
    gender VARCHAR NOT NULL,
    description VARCHAR
);

-- Create dependent tables for member

CREATE TABLE member_contact (
                                member_id BIGINT NOT NULL PRIMARY KEY,
                                email VARCHAR NOT NULL,
                                github_url VARCHAR,
                                linkedin_url VARCHAR,
                                phone_number VARCHAR NOT NULL,
                                updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                CONSTRAINT fk_member_contact_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE member_penalty (
                                member_id BIGINT NOT NULL PRIMARY KEY,
                                penalty_point INTEGER NOT NULL DEFAULT 0,
                                penaltied_at TIMESTAMPTZ NOT NULL,
                                updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                reason VARCHAR NOT NULL,
                                CONSTRAINT fk_member_penalty_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE auth (
                      member_id BIGINT NOT NULL PRIMARY KEY,
                      account_number VARCHAR NOT NULL UNIQUE,
                      password VARCHAR NOT NULL,
                      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                      deleted_at TIMESTAMPTZ,
                      CONSTRAINT fk_auth_member FOREIGN KEY (member_id) REFERENCES member(id)
);

-- Create main entity tables

CREATE TABLE study (
                       id BIGSERIAL PRIMARY KEY,
                       member_id BIGINT NOT NULL,
                       title VARCHAR NOT NULL,
                       content VARCHAR NOT NULL,
                       type VARCHAR NOT NULL,
                       skills TEXT[] NOT NULL,
                       category VARCHAR NOT NULL,
                       subcategory VARCHAR NOT NULL,
                       created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                       deleted_at TIMESTAMPTZ,
                       started_at TIMESTAMPTZ NOT NULL,
                       ended_at TIMESTAMPTZ NOT NULL,
                       max_participants_number INTEGER NOT NULL DEFAULT 5,
                       description VARCHAR NOT NULL,
                       github_url VARCHAR,
                       CONSTRAINT fk_study_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE project (
                         id BIGSERIAL PRIMARY KEY,
                         member_id BIGINT NOT NULL,
                         title VARCHAR NOT NULL,
                         content VARCHAR NOT NULL,
                         skills TEXT[] NOT NULL,
                         difficulty VARCHAR NOT NULL,
                         created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                         updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                         deleted_at TIMESTAMPTZ,
                         category VARCHAR NOT NULL,
                         description VARCHAR NOT NULL,
                         started_at TIMESTAMPTZ NOT NULL,
                         ended_at TIMESTAMPTZ NOT NULL,
                         max_participants_number INTEGER NOT NULL,
                         CONSTRAINT fk_project_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE schedule (
                          id BIGSERIAL PRIMARY KEY,
                          member_id BIGINT NOT NULL,
                          type VARCHAR NOT NULL,
                          title VARCHAR NOT NULL,
                          description VARCHAR NOT NULL,
                          content VARCHAR NOT NULL,
                          created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                          updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                          deleted_at TIMESTAMPTZ,
                          started_at TIMESTAMPTZ NOT NULL,
                          ended_at TIMESTAMPTZ NOT NULL,
                          CONSTRAINT fk_schedule_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE blog (
                      id BIGSERIAL PRIMARY KEY,
                      member_id BIGINT NOT NULL,
                      title VARCHAR NOT NULL,
                      content VARCHAR NOT NULL,
                      category VARCHAR NOT NULL,
                      is_published BOOLEAN NOT NULL DEFAULT FALSE,
                      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                      deleted_at TIMESTAMPTZ,
                      CONSTRAINT fk_blog_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE board (
                       id BIGSERIAL PRIMARY KEY,
                       member_id BIGINT NOT NULL,
                       title VARCHAR NOT NULL,
                       content VARCHAR NOT NULL,
                       category VARCHAR NOT NULL,
                       created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                       deleted_at TIMESTAMPTZ,
                       CONSTRAINT fk_board_member FOREIGN KEY (member_id) REFERENCES member(id)
);


CREATE TABLE blog_view (
                           id BIGSERIAL PRIMARY KEY,
                           blog_id BIGINT NOT NULL,
                           view_number INTEGER NOT NULL DEFAULT 0,
                           updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                           CONSTRAINT fk_blog_view_blog FOREIGN KEY (blog_id) REFERENCES blog(id)
);

-- Create dependent tables for board

CREATE TABLE board_attached (
                                id BIGSERIAL PRIMARY KEY,
                                board_id BIGINT NOT NULL,
                                member_id BIGINT NOT NULL,
                                attached_url VARCHAR NOT NULL,
                                name VARCHAR NOT NULL,
                                type VARCHAR NOT NULL,
                                size VARCHAR NOT NULL,
                                created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                deleted_at TIMESTAMPTZ,
                                CONSTRAINT fk_board_attached_board FOREIGN KEY (board_id) REFERENCES board(id),
                                CONSTRAINT fk_board_attached_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE board_like (
                            id BIGSERIAL PRIMARY KEY,
                            board_id BIGINT NOT NULL,
                            member_id BIGINT NOT NULL,
                            created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                            updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                            deleted_at TIMESTAMPTZ,
                            CONSTRAINT fk_board_like_board FOREIGN KEY (board_id) REFERENCES board(id),
                            CONSTRAINT fk_board_like_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE board_report (
                              id BIGSERIAL PRIMARY KEY,
                              board_id BIGINT NOT NULL,
                              member_id BIGINT NOT NULL,
                              content VARCHAR NOT NULL,
                              created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                              deleted_at TIMESTAMPTZ,
                              CONSTRAINT fk_board_report_board FOREIGN KEY (board_id) REFERENCES board(id),
                              CONSTRAINT fk_board_report_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE board_view (
                            id BIGSERIAL PRIMARY KEY,
                            board_id BIGINT NOT NULL,
                            view_number INTEGER NOT NULL DEFAULT 0,
                            updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                            CONSTRAINT fk_board_view_board FOREIGN KEY (board_id) REFERENCES board(id)
);

-- Create dependent tables for project

CREATE TABLE project_attached (
                                  id BIGSERIAL PRIMARY KEY,
                                  project_id BIGINT NOT NULL,
                                  member_id BIGINT NOT NULL,
                                  attached_url VARCHAR NOT NULL,
                                  name VARCHAR NOT NULL,
                                  type VARCHAR NOT NULL,
                                  size VARCHAR NOT NULL,
                                  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                  deleted_at TIMESTAMPTZ,
                                  CONSTRAINT fk_project_attached_project FOREIGN KEY (project_id) REFERENCES project(id),
                                  CONSTRAINT fk_project_attached_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE project_participant (
                                     id BIGSERIAL PRIMARY KEY,
                                     project_id BIGINT NOT NULL,
                                     member_id BIGINT NOT NULL,
                                     created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                     updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                     deleted_at TIMESTAMPTZ,
                                     CONSTRAINT fk_project_participant_project FOREIGN KEY (project_id) REFERENCES project(id),
                                     CONSTRAINT fk_project_participant_member FOREIGN KEY (member_id) REFERENCES member(id)
);


CREATE TABLE project_meeting (
                                 id BIGSERIAL PRIMARY KEY,
                                 project_id BIGINT NOT NULL,
                                 member_id BIGINT NOT NULL,
                                 title VARCHAR NOT NULL,
                                 content VARCHAR NOT NULL,
                                 participants TEXT[] NOT NULL,
                                 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                 updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                 deleted_at TIMESTAMPTZ,
                                 CONSTRAINT fk_project_meeting_project FOREIGN KEY (project_id) REFERENCES project(id),
                                 CONSTRAINT fk_project_meeting_member FOREIGN KEY (member_id) REFERENCES member(id)
);

-- Create dependent tables for schedule

CREATE TABLE schedule_attached (
                                   id BIGSERIAL PRIMARY KEY,
                                   schedule_id BIGINT NOT NULL,
                                   member_id BIGINT NOT NULL,
                                   attached_url VARCHAR NOT NULL,
                                   name VARCHAR NOT NULL,
                                   type VARCHAR NOT NULL,
                                   size VARCHAR NOT NULL,
                                   created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                   updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                   deleted_at TIMESTAMPTZ,
                                   CONSTRAINT fk_schedule_attached_schedule FOREIGN KEY (schedule_id) REFERENCES schedule(id),
                                   CONSTRAINT fk_schedule_attached_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE schedule_status (
                                 id BIGSERIAL PRIMARY KEY,
                                 schedule_id BIGINT NOT NULL,
                                 status VARCHAR NOT NULL DEFAULT 'PENDING',
                                 updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                 CONSTRAINT fk_schedule_status_schedule FOREIGN KEY (schedule_id) REFERENCES schedule(id)
);

-- Create dependent tables for study

CREATE TABLE study_attached (
                                id BIGSERIAL PRIMARY KEY,
                                study_id BIGINT NOT NULL,
                                member_id BIGINT NOT NULL,
                                attached_url VARCHAR NOT NULL,
                                name VARCHAR NOT NULL,
                                type VARCHAR NOT NULL,
                                size VARCHAR NOT NULL,
                                created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                deleted_at TIMESTAMPTZ,
                                CONSTRAINT fk_study_attached_study FOREIGN KEY (study_id) REFERENCES study(id),
                                CONSTRAINT fk_study_attached_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE study_meeting (
                               id BIGSERIAL PRIMARY KEY,
                               study_id BIGINT NOT NULL,
                               member_id BIGINT NOT NULL,
                               title VARCHAR NOT NULL,
                               content VARCHAR NOT NULL,
                               participants TEXT[] NOT NULL,
                               created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                               updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                               deleted_at TIMESTAMPTZ,
                               CONSTRAINT fk_study_meeting_study FOREIGN KEY (study_id) REFERENCES study(id),
                               CONSTRAINT fk_study_meeting_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE study_participant (
                                   id BIGSERIAL PRIMARY KEY,
                                   study_id BIGINT NOT NULL,
                                   member_id BIGINT NOT NULL,
                                   created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                   updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                   deleted_at TIMESTAMPTZ,
                                   CONSTRAINT fk_study_participant_study FOREIGN KEY (study_id) REFERENCES study(id),
                                   CONSTRAINT fk_study_participant_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE study_meeting_link (
                                    id BIGSERIAL PRIMARY KEY,
                                    study_id BIGINT NOT NULL,
                                    member_id BIGINT NOT NULL,
                                    name VARCHAR NOT NULL,
                                    attached_url VARCHAR NOT NULL,
                                    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                    deleted_at TIMESTAMPTZ,
                                    CONSTRAINT fk_study_meeting_link_study FOREIGN KEY (study_id) REFERENCES study(id),
                                    CONSTRAINT fk_study_meeting_link_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE project_meeting_link (
                                      id BIGSERIAL PRIMARY KEY,
                                      project_id BIGINT NOT NULL,
                                      member_id BIGINT NOT NULL,
                                      name VARCHAR NOT NULL,
                                      attached_url VARCHAR NOT NULL,
                                      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                      deleted_at TIMESTAMPTZ,
                                      CONSTRAINT fk_project_meeting_link_project FOREIGN KEY (project_id) REFERENCES project(id),
                                      CONSTRAINT fk_project_meeting_link_member FOREIGN KEY (member_id) REFERENCES member(id)
);

-- ✅ 스키마 생성 완료 로그
SELECT 'Schema creation completed successfully!' as status;

CREATE TABLE project_meeting_link (
                                      id BIGSERIAL PRIMARY KEY,
                                      project_id BIGINT NOT NULL,
                                      member_id BIGINT NOT NULL,
                                      name VARCHAR NOT NULL,
                                      attached_url VARCHAR NOT NULL,
                                      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                      deleted_at TIMESTAMPTZ,
                                      CONSTRAINT fk_project_meeting_link_project FOREIGN KEY (project_id) REFERENCES project(id),
                                      CONSTRAINT fk_project_meeting_link_member FOREIGN KEY (member_id) REFERENCES member(id)
);

major VARCHAR,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    birthday TIMESTAMPTZ NOT NULL,
    gender VARCHAR NOT NULL,
    description VARCHAR
);

-- Create dependent tables for member

CREATE TABLE member_contact (
    member_id BIGINT NOT NULL PRIMARY KEY,
    email VARCHAR NOT NULL,
    github_url VARCHAR,
    linkedin_url VARCHAR,
    phone_number VARCHAR NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_member_contact_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE member_penalty (
    member_id BIGINT NOT NULL PRIMARY KEY,
    penalty_point INTEGER NOT NULL DEFAULT 0,
    penaltied_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    reason VARCHAR NOT NULL,
    CONSTRAINT fk_member_penalty_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE auth (
    member_id BIGINT NOT NULL PRIMARY KEY,
    account_number VARCHAR NOT NULL UNIQUE,
    password VARCHAR NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_auth_member FOREIGN KEY (member_id) REFERENCES member(id)
);

-- Create main entity tables

CREATE TABLE study (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL,
    title VARCHAR NOT NULL,
    content VARCHAR NOT NULL,
    type VARCHAR NOT NULL,
    skills TEXT[] NOT NULL,
    category VARCHAR NOT NULL,
    subcategory VARCHAR NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    started_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ NOT NULL,
    max_participants_number INTEGER NOT NULL DEFAULT 5,
    description VARCHAR NOT NULL,
    github_url VARCHAR,
    CONSTRAINT fk_study_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE project (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL,
    title VARCHAR NOT NULL,
    content VARCHAR NOT NULL,
    skills TEXT[] NOT NULL,
    difficulty VARCHAR NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    category VARCHAR NOT NULL,
    description VARCHAR NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ NOT NULL,
    max_participants_number INTEGER NOT NULL,
    CONSTRAINT fk_project_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE schedule (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL,
    type VARCHAR NOT NULL,
    title VARCHAR NOT NULL,
    description VARCHAR NOT NULL,
    content VARCHAR NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    started_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_schedule_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE blog (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL,
    title VARCHAR NOT NULL,
    content VARCHAR NOT NULL,
    category VARCHAR NOT NULL,
    is_published BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_blog_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE board (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL,
    title VARCHAR NOT NULL,
    content VARCHAR NOT NULL,
    category VARCHAR NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_board_member FOREIGN KEY (member_id) REFERENCES member(id)
);


CREATE TABLE blog_view (
    id BIGSERIAL PRIMARY KEY,
    blog_id BIGINT NOT NULL,
    view_number INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_blog_view_blog FOREIGN KEY (blog_id) REFERENCES blog(id)
);

-- Create dependent tables for board

CREATE TABLE board_attached (
    id BIGSERIAL PRIMARY KEY,
    board_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    attached_url VARCHAR NOT NULL,
    name VARCHAR NOT NULL,
    type VARCHAR NOT NULL,
    size VARCHAR NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_board_attached_board FOREIGN KEY (board_id) REFERENCES board(id),
    CONSTRAINT fk_board_attached_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE board_like (
    id BIGSERIAL PRIMARY KEY,
    board_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_board_like_board FOREIGN KEY (board_id) REFERENCES board(id),
    CONSTRAINT fk_board_like_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE board_report (
    id BIGSERIAL PRIMARY KEY,
    board_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    content VARCHAR NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_board_report_board FOREIGN KEY (board_id) REFERENCES board(id),
    CONSTRAINT fk_board_report_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE board_view (
    id BIGSERIAL PRIMARY KEY,
    board_id BIGINT NOT NULL,
    view_number INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_board_view_board FOREIGN KEY (board_id) REFERENCES board(id)
);

-- Create dependent tables for project

CREATE TABLE project_attached (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    attached_url VARCHAR NOT NULL,
    name VARCHAR NOT NULL,
    type VARCHAR NOT NULL,
    size VARCHAR NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_project_attached_project FOREIGN KEY (project_id) REFERENCES project(id),
    CONSTRAINT fk_project_attached_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE project_participant (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_project_participant_project FOREIGN KEY (project_id) REFERENCES project(id),
    CONSTRAINT fk_project_participant_member FOREIGN KEY (member_id) REFERENCES member(id)
);


CREATE TABLE project_meeting (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    title VARCHAR NOT NULL,
    content VARCHAR NOT NULL,
    participants TEXT[] NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_project_meeting_project FOREIGN KEY (project_id) REFERENCES project(id),
    CONSTRAINT fk_project_meeting_member FOREIGN KEY (member_id) REFERENCES member(id)
);

-- Create dependent tables for schedule

CREATE TABLE schedule_attached (
    id BIGSERIAL PRIMARY KEY,
    schedule_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    attached_url VARCHAR NOT NULL,
    name VARCHAR NOT NULL,
    type VARCHAR NOT NULL,
    size VARCHAR NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_schedule_attached_schedule FOREIGN KEY (schedule_id) REFERENCES schedule(id),
    CONSTRAINT fk_schedule_attached_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE schedule_status (
    id BIGSERIAL PRIMARY KEY,
    schedule_id BIGINT NOT NULL,
    status VARCHAR NOT NULL DEFAULT 'PENDING',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_schedule_status_schedule FOREIGN KEY (schedule_id) REFERENCES schedule(id)
);

-- Create dependent tables for study

CREATE TABLE study_attached (
    id BIGSERIAL PRIMARY KEY,
    study_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    attached_url VARCHAR NOT NULL,
    name VARCHAR NOT NULL,
    type VARCHAR NOT NULL,
    size VARCHAR NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_study_attached_study FOREIGN KEY (study_id) REFERENCES study(id),
    CONSTRAINT fk_study_attached_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE study_meeting (
    id BIGSERIAL PRIMARY KEY,
    study_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    title VARCHAR NOT NULL,
    content VARCHAR NOT NULL,
    participants TEXT[] NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_study_meeting_study FOREIGN KEY (study_id) REFERENCES study(id),
    CONSTRAINT fk_study_meeting_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE study_participant (
    id BIGSERIAL PRIMARY KEY,
    study_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_study_participant_study FOREIGN KEY (study_id) REFERENCES study(id),
    CONSTRAINT fk_study_participant_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE study_meeting_link (
    id BIGSERIAL PRIMARY KEY,
    study_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    name VARCHAR NOT NULL,
    attached_url VARCHAR NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_study_meeting_link_study FOREIGN KEY (study_id) REFERENCES study(id),
    CONSTRAINT fk_study_meeting_link_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE project_meeting_link (
  id BIGSERIAL PRIMARY KEY,
  project_id BIGINT NOT NULL,
  member_id BIGINT NOT NULL,
  name VARCHAR NOT NULL,
  attached_url VARCHAR NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  deleted_at TIMESTAMPTZ,
  CONSTRAINT fk_project_meeting_link_project FOREIGN KEY (project_id) REFERENCES project(id),
  CONSTRAINT fk_project_meeting_link_member FOREIGN KEY (member_id) REFERENCES member(id)
);

-- ✅ 스키마 생성 완료 로그
SELECT 'Schema creation completed successfully!' as status; 