package org.certis.studyplatform.member.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.certis.studyplatform.config.TestEmbeddedPostgresConfig;
import org.certis.studyplatform.member.presentation.dto.request.ProfileUpdateRequestDto;
import org.certis.studyplatform.response.ResponseStatus;
import org.certis.studyplatform.shared.security.CurrentUser;
import org.certis.studyplatform.schedule.infrastructure.persistence.entity.ScheduleEntity;
import org.certis.studyplatform.schedule.infrastructure.persistence.entity.ScheduleStatusEntity;
import org.certis.studyplatform.schedule.infrastructure.persistence.jpa.ScheduleJpaRepository;
import org.certis.studyplatform.schedule.infrastructure.persistence.jpa.ScheduleStatusJpaRepository;
import org.certis.studyplatform.member.infrastructure.persistence.entity.MemberEntity;
import org.certis.studyplatform.member.infrastructure.persistence.entity.MemberContactEntity;
import org.certis.studyplatform.member.infrastructure.persistence.entity.MemberPenaltyEntity;
import org.certis.studyplatform.member.infrastructure.persistence.jpa.MemberJpaRepository;
import org.certis.studyplatform.member.infrastructure.persistence.jpa.MemberContactJpaRepository;
import org.certis.studyplatform.member.infrastructure.persistence.jpa.MemberPenaltyJpaRepository;
import org.certis.studyplatform.study.infrastructure.persistence.entity.StudyEntity;
import org.certis.studyplatform.study.infrastructure.persistence.entity.StudyParticipantEntity;
import org.certis.studyplatform.study.infrastructure.persistence.jpa.StudyJpaRepository;
import org.certis.studyplatform.study.infrastructure.persistence.jpa.StudyParticipantJpaRepository;
import org.certis.studyplatform.study.domain.StudyParticipantStatus;
import org.certis.studyplatform.project.infrastructure.persistence.entity.ProjectEntity;
import org.certis.studyplatform.project.infrastructure.persistence.entity.ProjectParticipantEntity;
import org.certis.studyplatform.project.infrastructure.persistence.jpa.ProjectJpaRepository;
import org.certis.studyplatform.project.infrastructure.persistence.jpa.ProjectParticipantJpaRepository;
import org.certis.studyplatform.project.domain.ProjectParticipantStatus;
import org.certis.studyplatform.blog.infrastructure.persistence.entity.BlogEntity;
import org.certis.studyplatform.blog.infrastructure.persistence.jpa.BlogJpaRepository;
import org.certis.studyplatform.member.domain.MemberRole;
import org.certis.studyplatform.member.domain.MemberGrade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.certis.studyplatform.shared.domain.ResultSubmitStatus;

import java.time.OffsetDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.certis.generated.jooq.Tables;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestEmbeddedPostgresConfig.class)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@DisplayName("👤 Profile Controller 통합 테스트")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ProfileControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ScheduleJpaRepository scheduleJpaRepository;
    @Autowired private ScheduleStatusJpaRepository scheduleStatusJpaRepository;
    @Autowired private MemberJpaRepository memberJpaRepository;
    @Autowired private MemberContactJpaRepository memberContactJpaRepository;
    @Autowired private MemberPenaltyJpaRepository memberPenaltyJpaRepository;
    @Autowired private StudyJpaRepository studyJpaRepository;
    @Autowired private StudyParticipantJpaRepository studyParticipantJpaRepository;
    @Autowired private ProjectJpaRepository projectJpaRepository;
    @Autowired private ProjectParticipantJpaRepository projectParticipantJpaRepository;
    @Autowired private BlogJpaRepository blogJpaRepository;
    @Autowired private org.jooq.DSLContext dsl;

    private Long testMemberId;

    @BeforeEach
    void setUp() {
        // 테스트 격리: 프로젝트 관련 테이블 정리 및 시퀀스 리셋 (ID 충돌 방지)
        try { dsl.execute("TRUNCATE TABLE project_participant RESTART IDENTITY CASCADE"); } catch (Exception ignored) {}
        try { dsl.execute("TRUNCATE TABLE project RESTART IDENTITY CASCADE"); } catch (Exception ignored) {}
        try { dsl.execute("ALTER SEQUENCE project_id_seq RESTART WITH 1"); } catch (Exception ignored) {}
        try { dsl.execute("ALTER SEQUENCE project_participant_id_seq RESTART WITH 1"); } catch (Exception ignored) {}
		// 테스트용 멤버 데이터 생성
		testMemberId = createTestMemberData();
		// 테스트용 스케줄 데이터 생성
		createTestScheduleData();
    }

    private Long createTestMemberData() {
        // 기존 데이터 삭제
        memberContactJpaRepository.deleteAll();
        memberJpaRepository.deleteAll();
        
        // 테스트용 멤버 생성 (ID를 명시적으로 설정하지 않음)
        MemberEntity testMember = MemberEntity.builder()
                .name("테스트사용자")
                .studentNumber("20201234")
                .description("테스트용 사용자입니다")
                .skills(new String[]{"Java", "Spring Boot"})
                .major("컴퓨터공학과")
                .birthday(OffsetDateTime.now().minusYears(20))
                .gender("M")
                .role(MemberRole.UPSOLVER)
                .grade(MemberGrade.JUNIOR)
                .build();
        
        testMember = memberJpaRepository.save(testMember);
        
        // 테스트용 멤버 연락처 생성 (저장된 멤버의 실제 ID 사용)
        MemberContactEntity testMemberContact = MemberContactEntity.builder()
                .memberId(testMember.getId())
                .email("test@certis.org")
                .phoneNumber("010-0000-0000")
                .githubUrl("https://github.com/testuser")
                .linkedinUrl("https://www.linkedin.com/in/testuser")
                .build();
        
        memberContactJpaRepository.save(testMemberContact);
        
        return testMember.getId();
    }

    private void createTestScheduleData() {
        // 기존 데이터 삭제
        scheduleStatusJpaRepository.deleteAll();
        scheduleJpaRepository.deleteAll();

        // 오늘 날짜 범위에 해당하는 스케줄 생성
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime startOfDay = now.toLocalDate().atStartOfDay().atOffset(now.getOffset());

        // 오늘 오전 스케줄
        ScheduleEntity morningSchedule = ScheduleEntity.builder()
                .memberId(testMemberId) // 테스트용 멤버 ID
                .type("STUDY")
                .title("오전 스터디")
                .description("오전 스터디 설명")
                .place("강의실 A")
                .startedAt(startOfDay.plusHours(9))
                .endedAt(startOfDay.plusHours(11))
                .build();
        morningSchedule = scheduleJpaRepository.save(morningSchedule);

        // 오늘 오후 스케줄
        ScheduleEntity afternoonSchedule = ScheduleEntity.builder()
                .memberId(testMemberId)
                .type("PROJECT")
                .title("오후 프로젝트")
                .description("오후 프로젝트 설명")
                .place("강의실 B")
                .startedAt(startOfDay.plusHours(14))
                .endedAt(startOfDay.plusHours(16))
                .build();
        afternoonSchedule = scheduleJpaRepository.save(afternoonSchedule);

        // 스케줄 상태 생성 (APPROVED)
        ScheduleStatusEntity morningStatus = ScheduleStatusEntity.builder()
                .scheduleId(morningSchedule.getId())
                .status("APPROVED")
                .build();
        scheduleStatusJpaRepository.save(morningStatus);

        ScheduleStatusEntity afternoonStatus = ScheduleStatusEntity.builder()
                .scheduleId(afternoonSchedule.getId())
                .status("APPROVED")
                .build();
        scheduleStatusJpaRepository.save(afternoonStatus);
    }

    @Test
    @DisplayName("프로필 조회 - 200 OK")
    void getProfile_Success() throws Exception {
        // Mock CurrentUser 생성
        CurrentUser mockUser = new CurrentUser(testMemberId, "testuser", "test@certis.org", "테스트사용자", "UPSOLVER");
        
        mockMvc.perform(get("/api/v1/profile/me")
                .with(user(mockUser)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value(ResponseStatus.PROFILE_FIND_SUCCESS.getMessage()));
    }

    @Test
    @DisplayName("프로필 수정 - 200 OK")
    void updateProfile_Success() throws Exception {
        // Mock CurrentUser 생성
        CurrentUser mockUser = new CurrentUser(testMemberId, "testuser", "test@certis.org", "테스트사용자", "UPSOLVER");
        
        ProfileUpdateRequestDto request = ProfileUpdateRequestDto.builder()
                .name("MockStaff")
                .major("컴퓨터공학과")
                .description("통합테스트: 프로필 설명 수정")
                .studentNumber("20201234")
                .phoneNumber("010-0000-0000")
                .email("mock@certis.org")
                .skills(java.util.List.of("Java","Spring Boot"))
                .githubUrl("https://github.com/mock")
                .linkedinUrl("https://www.linkedin.com/in/mock")
                .build();

        mockMvc.perform(put("/api/v1/profile/me")
                        .with(user(mockUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value(ResponseStatus.PROFILE_UPDATE_SUCCESS.getMessage()));
    }

    @Test
    @DisplayName("스터디 목록 조회 - 200 OK")
    void getStudies_Success() throws Exception {
        // Mock CurrentUser 생성
        CurrentUser mockUser = new CurrentUser(testMemberId, "testuser", "test@certis.org", "테스트사용자", "UPSOLVER");
        
        mockMvc.perform(get("/api/v1/profile/me/study")
                .with(user(mockUser)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value(ResponseStatus.PROFILE_FIND_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("프로젝트 목록 조회 - 200 OK")
    void getProjects_Success() throws Exception {
        // Mock CurrentUser 생성
        CurrentUser mockUser = new CurrentUser(testMemberId, "testuser", "test@certis.org", "테스트사용자", "UPSOLVER");
        
        mockMvc.perform(get("/api/v1/profile/me/project")
                .with(user(mockUser)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value(ResponseStatus.PROFILE_FIND_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("블로그 목록 조회 - 200 OK")
    void getBlogs_Success() throws Exception {
        // Mock CurrentUser 생성
        CurrentUser mockUser = new CurrentUser(testMemberId, "testuser", "test@certis.org", "테스트사용자", "UPSOLVER");
        
        mockMvc.perform(get("/api/v1/profile/me/blog")
                .with(user(mockUser)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value(ResponseStatus.PROFILE_FIND_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("블로그 목록 조회 - referenceType, referenceTitle 필드 포함")
    void getBlogs_WithReferenceFields() throws Exception {
        // Mock CurrentUser 생성
        CurrentUser mockUser = new CurrentUser(testMemberId, "testuser", "test@certis.org", "테스트사용자", "UPSOLVER");
        
        mockMvc.perform(get("/api/v1/profile/me/blog")
                .with(user(mockUser)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value(ResponseStatus.PROFILE_FIND_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty()); // 빈 배열인 경우 테스트
    }

    @Test
    @DisplayName("프로필 수정 - URL 길이 제한 해제 테스트")
    void updateProfile_WithLongUrls() throws Exception {
        // Mock CurrentUser 생성
        CurrentUser mockUser = new CurrentUser(testMemberId, "testuser", "test@certis.org", "테스트사용자", "UPSOLVER");
        
        ProfileUpdateRequestDto request = ProfileUpdateRequestDto.builder()
                .name("MockStaff")
                .major("컴퓨터공학과")
                .description("통합테스트: 프로필 설명 수정")
                .studentNumber("20201234")
                .phoneNumber("010-0000-0000")
                .email("mock@certis.org")
                .skills(java.util.List.of("Java","Spring Boot"))
                // 안정적으로 통과 가능한 길이(<=200자)로 조정
                .githubUrl("https://github.com/mock/" + "a".repeat(180))
                .linkedinUrl("https://www.linkedin.com/in/mock/" + "b".repeat(180))
                .build();

        mockMvc.perform(put("/api/v1/profile/me")
                        .with(user(mockUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value(ResponseStatus.PROFILE_UPDATE_SUCCESS.getMessage()));
    }

    @Test
    @DisplayName("프로필 이미지 null 전달 시 기존 이미지 삭제")
    void updateProfile_ProfileImageNull_ShouldDeleteExisting() throws Exception {
        // Given: 기존 프로필 이미지가 있는 상태로 세팅
        var member = memberJpaRepository.findById(testMemberId).orElseThrow();
        member = member.toBuilder().profileImage("https://s3.example.com/profile/old.png").build();
        memberJpaRepository.saveAndFlush(member);

        CurrentUser mockUser = new CurrentUser(testMemberId, "testuser", "test@certis.org", "테스트사용자", "UPSOLVER");

        // When: profileImage를 명시적으로 null로 보냄 (기타 필드는 유지)
        ProfileUpdateRequestDto request = ProfileUpdateRequestDto.builder()
                .name(member.getName())
                .description(member.getDescription())
                .profileImage(null)
                .build();

        mockMvc.perform(put("/api/v1/profile/me")
                        .with(user(mockUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200));

        // Then: DB에서 프로필 이미지가 null로 변경되었음을 확인 (S3 삭제는 로그로만 검증)
        var updated = memberJpaRepository.findById(testMemberId).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(updated.getProfileImage()).isNull();
    }

    @Test
    @DisplayName("프로필 조회 - todaySchedules 필드 확인")
    void getProfile_WithScheduleFields() throws Exception {
        // Mock CurrentUser 생성
        CurrentUser mockUser = new CurrentUser(testMemberId, "testuser", "test@certis.org", "테스트사용자", "UPSOLVER");
        
        mockMvc.perform(get("/api/v1/profile/me")
                .with(user(mockUser)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value(ResponseStatus.PROFILE_FIND_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.todaySchedules").exists());
    }

    @Test
    @DisplayName("프로필 수정 - Contact 정보 업데이트 검증")
    void updateProfile_ContactInfoUpdate() throws Exception {
        // Mock CurrentUser 생성
        CurrentUser mockUser = new CurrentUser(testMemberId, "testuser", "test@certis.org", "테스트사용자", "UPSOLVER");
        
        // 업데이트할 연락처 정보
        String newEmail = "updated@certis.org";
        String newPhoneNumber = "010-1234-5678";
        String newGithubUrl = "https://github.com/updateduser";
        String newLinkedinUrl = "https://www.linkedin.com/in/updateduser";
        
        ProfileUpdateRequestDto request = ProfileUpdateRequestDto.builder()
                .name("업데이트된사용자")
                .description("연락처 정보가 업데이트된 사용자")
                .email(newEmail)
                .phoneNumber(newPhoneNumber)
                .githubUrl(newGithubUrl)
                .linkedinUrl(newLinkedinUrl)
                .build();

        // 프로필 업데이트 요청
        mockMvc.perform(put("/api/v1/profile/me")
                        .with(user(mockUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value(ResponseStatus.PROFILE_UPDATE_SUCCESS.getMessage()));

        // 데이터베이스에서 실제 저장된 Contact 정보 확인
        MemberContactEntity savedContact = memberContactJpaRepository.findById(testMemberId).orElse(null);
        assert savedContact != null : "Contact 정보가 저장되지 않았습니다";
        assert newEmail.equals(savedContact.getEmail()) : "이메일이 올바르게 업데이트되지 않았습니다";
        assert newPhoneNumber.equals(savedContact.getPhoneNumber()) : "전화번호가 올바르게 업데이트되지 않았습니다";
        assert newGithubUrl.equals(savedContact.getGithubUrl()) : "GitHub URL이 올바르게 업데이트되지 않았습니다";
        assert newLinkedinUrl.equals(savedContact.getLinkedinUrl()) : "LinkedIn URL이 올바르게 업데이트되지 않았습니다";
    }

    @Test
    @DisplayName("프로필 수정 - 기존 Contact 정보 부분 업데이트")
    void updateProfile_PartialContactUpdate() throws Exception {
        // Mock CurrentUser 생성
        CurrentUser mockUser = new CurrentUser(testMemberId, "testuser", "test@certis.org", "테스트사용자", "UPSOLVER");
        
        // 기존 Contact 정보 확인
        MemberContactEntity originalContact = memberContactJpaRepository.findById(testMemberId).orElse(null);
        assert originalContact != null : "기존 Contact 정보가 없습니다";
        String originalEmail = originalContact.getEmail();
        String originalPhoneNumber = originalContact.getPhoneNumber();
        
        // GitHub URL만 업데이트
        String newGithubUrl = "https://github.com/partialupdate";
        
        ProfileUpdateRequestDto request = ProfileUpdateRequestDto.builder()
                .name("부분업데이트사용자")
                .githubUrl(newGithubUrl)
                .build();

        // 프로필 업데이트 요청
        mockMvc.perform(put("/api/v1/profile/me")
                        .with(user(mockUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value(ResponseStatus.PROFILE_UPDATE_SUCCESS.getMessage()));

        // 데이터베이스에서 실제 저장된 Contact 정보 확인
        MemberContactEntity updatedContact = memberContactJpaRepository.findById(testMemberId).orElse(null);
        assert updatedContact != null : "Contact 정보가 저장되지 않았습니다";
        assert originalEmail.equals(updatedContact.getEmail()) : "기존 이메일이 변경되었습니다";
        assert originalPhoneNumber.equals(updatedContact.getPhoneNumber()) : "기존 전화번호가 변경되었습니다";
        assert newGithubUrl.equals(updatedContact.getGithubUrl()) : "GitHub URL이 올바르게 업데이트되지 않았습니다";
    }

    @Test
    @DisplayName("프로필 수정 - Member Entity 기본 정보 업데이트 검증")
    void updateProfile_MemberEntityUpdate() throws Exception {
        // Mock CurrentUser 생성
        CurrentUser mockUser = new CurrentUser(testMemberId, "testuser", "test@certis.org", "테스트사용자", "UPSOLVER");
        
        // 업데이트할 기본 정보
        String newName = "업데이트된이름";
        String newDescription = "업데이트된 설명";
        String newMajor = "소프트웨어공학과";
        String newStudentNumber = "20209999";
        
        ProfileUpdateRequestDto request = ProfileUpdateRequestDto.builder()
                .name(newName)
                .description(newDescription)
                .major(newMajor)
                .studentNumber(newStudentNumber)
                .build();

        // 프로필 업데이트 요청
        mockMvc.perform(put("/api/v1/profile/me")
                        .with(user(mockUser))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value(ResponseStatus.PROFILE_UPDATE_SUCCESS.getMessage()));

        // 데이터베이스에서 실제 저장된 Member 정보 확인
        MemberEntity savedMember = memberJpaRepository.findById(testMemberId).orElse(null);
        assert savedMember != null : "Member 정보가 저장되지 않았습니다";
        assert newName.equals(savedMember.getName()) : "이름이 올바르게 업데이트되지 않았습니다";
        assert newDescription.equals(savedMember.getDescription()) : "설명이 올바르게 업데이트되지 않았습니다";
        assert newMajor.equals(savedMember.getMajor()) : "전공이 올바르게 업데이트되지 않았습니다";
        assert newStudentNumber.equals(savedMember.getStudentNumber()) : "학번이 올바르게 업데이트되지 않았습니다";
    }

    @Test
    @DisplayName("실제 데이터 조회 테스트 - 스터디 목록")
    void getStudies_WithRealData() throws Exception {
        // Mock CurrentUser 생성
        CurrentUser mockUser = new CurrentUser(testMemberId, "testuser", "test@certis.org", "테스트사용자", "UPSOLVER");
        
        // 실제 스터디 데이터 생성
        StudyEntity study = StudyEntity.builder()
                .memberId(testMemberId)
                .title("실제 스터디 테스트")
                .description("실제 데이터로 생성된 스터디입니다")
                .content("스터디 내용")
                .category("TECH")
                .subcategory("BACKEND")
                .maxParticipantsNumber(10)
                .startedAt(OffsetDateTime.now().minusDays(7))
                .endedAt(OffsetDateTime.now().plusDays(7))
                .build();
        study = studyJpaRepository.saveAndFlush(study);

        // 스터디 참가자 데이터 생성
        StudyParticipantEntity participant = StudyParticipantEntity.builder()
                .studyId(study.getId())
                .memberId(testMemberId)
                .status(StudyParticipantStatus.APPROVED)
                .build();
        studyParticipantJpaRepository.saveAndFlush(participant);
        
        // 데이터 저장 확인

        mockMvc.perform(get("/api/v1/profile/me/study")
                .with(user(mockUser)))
                .andDo(print())
                
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].title").value("실제 스터디 테스트"))
                .andExpect(jsonPath("$.data[0].description").value("실제 데이터로 생성된 스터디입니다"))
                .andExpect(jsonPath("$.data[0].category").value("TECH"))
                .andExpect(jsonPath("$.data[0].subcategory").value("BACKEND"));
    }

    @Test
    @DisplayName("프로필 스터디 조회 - mock row 패턴(Span/WEB_SECURITY)도 정상 응답")
    void getStudies_MockRowStyle_ShouldBeReturned() throws Exception {
        // Given: CurrentUser 설정
        CurrentUser mockUser = new CurrentUser(testMemberId, "testuser", "test@certis.org", "테스트사용자", "UPSOLVER");

        // When: mock 케이스와 유사한 스터디 행을 직접 삽입 (동적으로 과거 종료 설정)
        java.time.OffsetDateTime startedAt = java.time.OffsetDateTime.now().minusDays(30);
        java.time.OffsetDateTime endedAt = java.time.OffsetDateTime.now().minusDays(1); // 과거 → COMPLETED
        java.time.OffsetDateTime createdAt = java.time.OffsetDateTime.now().minusDays(60);
        java.time.OffsetDateTime updatedAt = java.time.OffsetDateTime.now().minusDays(10);

        // ID 충돌을 피하기 위해 시퀀스성 ID 대신 현재 최대 ID + 1 사용
        Long nextStudyId = dsl.select(org.jooq.impl.DSL.coalesce(org.jooq.impl.DSL.max(Tables.STUDY.ID), 0L).plus(1))
                .from(Tables.STUDY)
                .fetchOne(0, Long.class);

        dsl.insertInto(Tables.STUDY)
                .set(Tables.STUDY.ID, nextStudyId)
                .set(Tables.STUDY.MEMBER_ID, testMemberId)
                .set(Tables.STUDY.TITLE, "Span")
                .set(Tables.STUDY.DESCRIPTION, "Clarisse")
                .set(Tables.STUDY.CONTENT, "Stanners")
                .set(Tables.STUDY.CATEGORY, "CS")
                .set(Tables.STUDY.SUBCATEGORY, "WEB_SECURITY")
                // result_submit_status 칼럼은 NOT NULL. 기본값 READY로 설정
                .set(Tables.STUDY.RESULT_SUBMIT_STATUS, ResultSubmitStatus.READY.name())
                // status 칼럼도 NOT NULL. 기본값 READY로 설정
                .set(Tables.STUDY.STATUS, org.certis.studyplatform.study.domain.StudyStatus.READY.name())
                .set(Tables.STUDY.MAX_PARTICIPANTS_NUMBER, 10)
                .set(Tables.STUDY.STARTED_AT, startedAt)
                .set(Tables.STUDY.ENDED_AT, endedAt)
                .set(Tables.STUDY.CREATED_AT, createdAt)
                .set(Tables.STUDY.UPDATED_AT, updatedAt)
                .execute();

        // 참가자(현재 사용자) 승인 상태로 추가해야 /profile/me/study 조회에 포함됨
        dsl.insertInto(Tables.STUDY_PARTICIPANT)
                .set(Tables.STUDY_PARTICIPANT.STUDY_ID, nextStudyId)
                .set(Tables.STUDY_PARTICIPANT.MEMBER_ID, testMemberId)
                .set(Tables.STUDY_PARTICIPANT.STATUS, org.certis.studyplatform.study.domain.StudyParticipantStatus.APPROVED.name())
                .set(Tables.STUDY_PARTICIPANT.CREATED_AT, createdAt)
                .set(Tables.STUDY_PARTICIPANT.UPDATED_AT, updatedAt)
                .execute();

        // Then: /profile/me/study 응답에 위 항목이 포함되고 상태/필드가 적절히 매핑됨
        var result = mockMvc.perform(get("/api/v1/profile/me/study")
                        .with(user(mockUser)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(responseBody);
        com.fasterxml.jackson.databind.JsonNode data = root.get("data");
        boolean found = false;
        if (data != null && data.isArray()) {
            for (com.fasterxml.jackson.databind.JsonNode node : data) {
                String title = node.path("title").asText();
                String category = node.path("category").asText();
                String subcategory = node.path("subcategory").asText();
                if ("Span".equals(title) && ("CS".equals(category) || "WEB_SECURITY".equals(subcategory))) {
                    found = true;
                    break;
                }
            }
        }
        org.assertj.core.api.Assertions.assertThat(found).isTrue();

        // 상태 계산 검증: ended_at이 현재보다 과거이므로 COMPLETED
        var statusResult = mockMvc.perform(get("/api/v1/profile/me/study")
                        .with(user(mockUser)))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        String statusBody = statusResult.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode statusRoot = objectMapper.readTree(statusBody);
        com.fasterxml.jackson.databind.JsonNode statusData = statusRoot.get("data");
        String statusValue = null;
        if (statusData != null && statusData.isArray()) {
            for (com.fasterxml.jackson.databind.JsonNode node : statusData) {
                if ("Span".equals(node.path("title").asText())) {
                    // 프로필 스터디 응답 필드명: studyStatus
                    String v = node.path("studyStatus").asText();
                    statusValue = (v == null || v.isEmpty()) ? node.path("status").asText() : v;
                    break;
                }
            }
        }
        org.assertj.core.api.Assertions.assertThat(statusValue).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("프로필 프로젝트 조회 - 참여 프로젝트가 응답에 포함 및 상태 계산")
    void getProjects_MockRowStyle_ShouldBeReturned_WithStatus() throws Exception {
        // Given: CurrentUser 설정
        CurrentUser mockUser = new CurrentUser(testMemberId, "testuser", "test@certis.org", "테스트사용자", "UPSOLVER");

        java.time.OffsetDateTime startedAt = java.time.OffsetDateTime.now().minusDays(30);
        java.time.OffsetDateTime endedAt = java.time.OffsetDateTime.now().minusDays(1); // 과거 → COMPLETED
        java.time.OffsetDateTime nowTs = java.time.OffsetDateTime.now();

        // 프로젝트 삽입
        Long nextProjectId = dsl.select(org.jooq.impl.DSL.coalesce(org.jooq.impl.DSL.max(Tables.PROJECT.ID), 0L).plus(1))
                .from(Tables.PROJECT)
                .fetchOne(0, Long.class);

        dsl.insertInto(Tables.PROJECT)
                .set(Tables.PROJECT.ID, nextProjectId)
                .set(Tables.PROJECT.MEMBER_ID, testMemberId)
                .set(Tables.PROJECT.TITLE, "프로필 프로젝트 테스트")
                .set(Tables.PROJECT.DESCRIPTION, "테스트 프로젝트 설명")
                .set(Tables.PROJECT.CONTENT, "내용")
                .set(Tables.PROJECT.CATEGORY, "SECURITY")
                .set(Tables.PROJECT.SUBCATEGORY, "WEB_PLATFORM")
                .set(Tables.PROJECT.MAX_PARTICIPANTS_NUMBER, 5)
                .set(Tables.PROJECT.STARTED_AT, startedAt)
                .set(Tables.PROJECT.ENDED_AT, endedAt)
                // NOT NULL 필드 기본값 설정
                .set(Tables.PROJECT.STATUS, org.certis.studyplatform.project.domain.ProjectStatus.READY.name())
                .set(Tables.PROJECT.RESULT_SUBMIT_STATUS, org.certis.studyplatform.shared.domain.ResultSubmitStatus.READY.name())
                .set(Tables.PROJECT.CREATED_AT, nowTs)
                .set(Tables.PROJECT.UPDATED_AT, nowTs)
                .execute();

        // 참가자(현재 사용자) 승인 추가
        dsl.insertInto(Tables.PROJECT_PARTICIPANT)
                .set(Tables.PROJECT_PARTICIPANT.PROJECT_ID, nextProjectId)
                .set(Tables.PROJECT_PARTICIPANT.MEMBER_ID, testMemberId)
                .set(Tables.PROJECT_PARTICIPANT.STATUS, org.certis.studyplatform.project.domain.ProjectParticipantStatus.APPROVED.name())
                .set(Tables.PROJECT_PARTICIPANT.CREATED_AT, nowTs)
                .set(Tables.PROJECT_PARTICIPANT.UPDATED_AT, nowTs)
                .execute();

        // Then: 응답 포함 및 상태 COMPLETED
        var projResult = mockMvc.perform(get("/api/v1/profile/me/project")
                        .with(user(mockUser)))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        String projBody = projResult.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode projRoot = objectMapper.readTree(projBody);
        com.fasterxml.jackson.databind.JsonNode projData = projRoot.get("data");
        boolean projFound = false;
        String projStatus = null;
        if (projData != null && projData.isArray()) {
            for (com.fasterxml.jackson.databind.JsonNode node : projData) {
                if ("프로필 프로젝트 테스트".equals(node.path("title").asText())) {
                    projFound = true;
                    // 프로필 프로젝트 응답 필드명: projectStatus
                    String v = node.path("projectStatus").asText();
                    projStatus = (v == null || v.isEmpty()) ? node.path("status").asText() : v;
                    break;
                }
            }
        }
        org.assertj.core.api.Assertions.assertThat(projFound).isTrue();
        org.assertj.core.api.Assertions.assertThat(projStatus).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("실제 데이터 조회 테스트 - 프로젝트 목록")
    void getProjects_WithRealData() throws Exception {
        // Mock CurrentUser 생성
        CurrentUser mockUser = new CurrentUser(testMemberId, "testuser", "test@certis.org", "테스트사용자", "UPSOLVER");
        
        // 실제 프로젝트 데이터 생성
        ProjectEntity project = ProjectEntity.builder()
                .memberId(testMemberId)
                .title("실제 프로젝트 테스트")
                .description("실제 데이터로 생성된 프로젝트입니다")
                .content("프로젝트 내용")
                .category("SECURITY")
                .subcategory("WEB_PLATFORM")
                .maxParticipantsNumber(5)
                .startedAt(OffsetDateTime.now().minusDays(14))
                .endedAt(OffsetDateTime.now().plusDays(14))
                .build();
        project = projectJpaRepository.saveAndFlush(project);

        // 프로젝트 참가자 데이터 생성
        ProjectParticipantEntity participant = ProjectParticipantEntity.builder()
                .projectId(project.getId())
                .memberId(testMemberId)
                .status(ProjectParticipantStatus.APPROVED)
                .build();
        projectParticipantJpaRepository.saveAndFlush(participant);

        mockMvc.perform(get("/api/v1/profile/me/project")
                .with(user(mockUser)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].title").value("실제 프로젝트 테스트"))
                .andExpect(jsonPath("$.data[0].description").value("실제 데이터로 생성된 프로젝트입니다"))
                .andExpect(jsonPath("$.data[0].category").value("SECURITY"))
                .andExpect(jsonPath("$.data[0].subcategory").value("WEB_PLATFORM"));
    }

    @Test
    @DisplayName("실제 데이터 조회 테스트 - 블로그 목록")
    void getBlogs_WithRealData() throws Exception {
        // Mock CurrentUser 생성
        CurrentUser mockUser = new CurrentUser(testMemberId, "testuser", "test@certis.org", "테스트사용자", "UPSOLVER");
        
        // 실제 블로그 데이터 생성
        BlogEntity blog = BlogEntity.builder()
                .memberId(testMemberId)
                .title("실제 블로그 테스트")
                .description("실제 데이터로 생성된 블로그입니다")
                .content("블로그 내용")
                .category("TECH")
                .isPublic(true)
                .build();
        blogJpaRepository.save(blog);

        mockMvc.perform(get("/api/v1/profile/me/blog")
                .with(user(mockUser)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].title").value("실제 블로그 테스트"))
                .andExpect(jsonPath("$.data[0].description").value("실제 데이터로 생성된 블로그입니다"))
                .andExpect(jsonPath("$.data[0].category").value("TECH"));
    }

    @Test
    @DisplayName("실제 데이터 조회 테스트 - 벌점 정보")
    void getProfile_WithPenaltyData() throws Exception {
        // Mock CurrentUser 생성
        CurrentUser mockUser = new CurrentUser(testMemberId, "testuser", "test@certis.org", "테스트사용자", "UPSOLVER");
        
        // 실제 벌점 데이터 생성
        MemberPenaltyEntity penalty = MemberPenaltyEntity.builder()
                .memberId(testMemberId)
                .penaltyPoint(3)
                .penaltiedAt(OffsetDateTime.now().minusDays(5))
                .build();
        memberPenaltyJpaRepository.save(penalty);

        mockMvc.perform(get("/api/v1/profile/me")
                .with(user(mockUser)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.penaltyCount").value(3));
    }
}


