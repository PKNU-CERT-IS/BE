-- Insert 3 manipulable admin accounts for testing

-- Admin Account 1: Super Admin
insert into member (id, name, student_number, description, profile_image, grade, role, skills, major, birthday, gender, grace_period, created_at, updated_at, deleted_at) 
values (9001, 'Admin Super', '202590001', 'Super Administrator for CERT-IS Platform', 'http://example.com/admin1.jpg', 'GRADUATED', 'ADMIN', '{Platform Management, Security, System Administration}', 'COMPUTER', '1990-01-01 00:00:00', 'MALE', null, '2025-09-16 09:00:00', '2025-09-16 09:00:00', null);

-- Admin Account 2: Content Admin  
insert into member (id, name, student_number, description, profile_image, grade, role, skills, major, birthday, gender, grace_period, created_at, updated_at, deleted_at) 
values (9002, 'Admin Content', '202590002', 'Content Administrator for CERT-IS Platform', 'http://example.com/admin2.jpg', 'GRADUATED', 'ADMIN', '{Content Management, Moderation, User Support}', 'COMPUTER', '1992-05-15 00:00:00', 'FEMALE', null, '2025-09-16 09:00:00', '2025-09-16 09:00:00', null);

-- Admin Account 3: Technical Admin
insert into member (id, name, student_number, description, profile_image, grade, role, skills, major, birthday, gender, grace_period, created_at, updated_at, deleted_at) 
values (9003, 'Admin Tech', '202590003', 'Technical Administrator for CERT-IS Platform', 'http://example.com/admin3.jpg', 'GRADUATED', 'ADMIN', '{System Maintenance, Database Management, API Development}', 'COMPUTER', '1988-12-30 00:00:00', 'MALE', null, '2025-09-16 09:00:00', '2025-09-16 09:00:00', null);

-- Insert corresponding auth records with BCrypt hashed password "1234"
insert into auth (member_id, account_number, password, created_at, updated_at, deleted_at) 
values (9001, 'ADMIN001', '$2a$10$N9qo8uLOickgx2ZMRZoMye1J2LyGgJ/YJwCwGJF1Qn7xPKgNBdJZW', '2025-09-16 09:00:00', '2025-09-16 09:00:00', null);

insert into auth (member_id, account_number, password, created_at, updated_at, deleted_at) 
values (9002, 'ADMIN002', '$2a$10$N9qo8uLOickgx2ZMRZoMye1J2LyGgJ/YJwCwGJF1Qn7xPKgNBdJZW', '2025-09-16 09:00:00', '2025-09-16 09:00:00', null);

insert into auth (member_id, account_number, password, created_at, updated_at, deleted_at) 
values (9003, 'ADMIN003', '$2a$10$N9qo8uLOickgx2ZMRZoMye1J2LyGgJ/YJwCwGJF1Qn7xPKgNBdJZW', '2025-09-16 09:00:00', '2025-09-16 09:00:00', null);

-- Insert member contact information for admin accounts
insert into member_contact (member_id, phone_number, email, github_url, linked_url, created_at, updated_at, deleted_at)
values (9001, '010-0000-0001', 'admin.super@certis.org', 'https://github.com/certis-admin1', 'https://linkedin.com/in/certis-admin1', '2025-09-16 09:00:00', '2025-09-16 09:00:00', null);

insert into member_contact (member_id, phone_number, email, github_url, linked_url, created_at, updated_at, deleted_at)
values (9002, '010-0000-0002', 'admin.content@certis.org', 'https://github.com/certis-admin2', 'https://linkedin.com/in/certis-admin2', '2025-09-16 09:00:00', '2025-09-16 09:00:00', null);

insert into member_contact (member_id, phone_number, email, github_url, linked_url, created_at, updated_at, deleted_at)
values (9003, '010-0000-0003', 'admin.tech@certis.org', 'https://github.com/certis-admin3', 'https://linkedin.com/in/certis-admin3', '2025-09-16 09:00:00', '2025-09-16 09:00:00', null);

-- Note: The password hash above corresponds to "1234" for easy testing
-- Account numbers: ADMIN001, ADMIN002, ADMIN003
-- Passwords: 1234 (for all three accounts)
-- BCrypt hash: $2a$10$N9qo8uLOickgx2ZMRZoMye1J2LyGgJ/YJwCwGJF1Qn7xPKgNBdJZW
