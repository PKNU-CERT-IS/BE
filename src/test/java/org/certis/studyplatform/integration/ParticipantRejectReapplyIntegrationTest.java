package org.certis.studyplatform.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.certis.studyplatform.config.TestEmbeddedPostgresConfig;
import org.certis.studyplatform.config.TestWebMvcConfig;
import org.certis.studyplatform.project.domain.ProjectParticipantStatus;
import org.certis.studyplatform.project.presentation.dto.request.ProjectJoinRequestDto;
import org.certis.studyplatform.project.presentation.dto.request.ProjectJoinRejectRequestDto;
import org.certis.studyplatform.shared.security.CurrentUser;
import org.certis.studyplatform.study.domain.StudyParticipantStatus;
import org.certis.studyplatform.study.presentation.dto.request.StudyJoinRequestDto;
import org.certis.studyplatform.study.presentation.dto.request.StudyJoinRejectRequestDto;
import org.jooq.DSLContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.certis.generated.jooq.Tables.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Participant Reject 후 Reapply 통합 테스트
 * 
 * 🎯 테스트 목표:
 * - 참가 신청 → 거절 → 재신청 시나리오 검증
 * - 생성자와 관리자 모두의 거절 권한 검증
 * - 소프트 삭제 후 중복 체크 우회 검증
 * 
 * 🔧 테스트 시나리오:
 * 1. 스터디 참가 신청 → 생성자 거절 → 다시 신청 (성공)
 * 2. 프로젝트 참가 신청 → 생성자 거절 → 다시 신청 (성공)
 * 3. 스터디 참가 신청 → 관리자 거절 → 다시 신청 (성공)
 * 4. 프로젝트 참가 신청 → 관리자 거절 → 다시 신청 (성공)
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestEmbeddedPostgresConfig.class, TestWebMvcConfig.class})
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:postgresql://localhost:5432/testdb",
    "spring.datasource.username=test",
    "spring.datasource.password=test"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Disabled("Temporarily disabled to run only domain participant limit tests")
class ParticipantRejectReapplyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DSLContext dsl;

    // 테스트용 상수
    private static final Long TEST_STUDY_ID = 1L;
    private static final Long TEST_PROJECT_ID = 1L;
    private static Long CREATOR_ID; // 스터디/프로젝트 생성자
    private static Long PARTICIPANT_ID; // 참가 신청자
    private static Long ADMIN_ID; // 관리자

    @BeforeEach
    void setUp() {
        // 테스트 데이터 초기화
        initializeTestData();
    }

    @AfterEach
    void tearDown() {
        // 테스트 데이터 정리
        cleanupTestData();
    }

    /**
     * 테스트 데이터 초기화
     */
    private void initializeTestData() {
        // 기존 데이터 정리
        dsl.deleteFrom(STUDY_PARTICIPANT).execute();
        dsl.deleteFrom(PROJECT_PARTICIPANT).execute();
        dsl.deleteFrom(STUDY).execute();
        dsl.deleteFrom(PROJECT).execute();
        dsl.deleteFrom(MEMBER).execute();

        // 멤버 데이터 생성
        dsl.insertInto(MEMBER)
                .set(MEMBER.ID, 1L)
                .set(MEMBER.NAME, "생성자")
                .set(MEMBER.STUDENT_NUMBER, "20210001")
                .set(MEMBER.MAJOR, "컴퓨터공학과")
                .set(MEMBER.ROLE, "UPSOLVER")
                .set(MEMBER.GRADE, "JUNIOR")
                .set(MEMBER.BIRTHDAY, OffsetDateTime.now().minusYears(20))
                .set(MEMBER.GENDER, "MALE")
                .set(MEMBER.CREATED_AT, OffsetDateTime.now())
                .set(MEMBER.UPDATED_AT, OffsetDateTime.now())
                .execute();

        dsl.insertInto(MEMBER)
                .set(MEMBER.ID, 2L)
                .set(MEMBER.NAME, "참가자")
                .set(MEMBER.STUDENT_NUMBER, "20210002")
                .set(MEMBER.MAJOR, "컴퓨터공학과")
                .set(MEMBER.ROLE, "UPSOLVER")
                .set(MEMBER.GRADE, "SOPHOMORE")
                .set(MEMBER.BIRTHDAY, OffsetDateTime.now().minusYears(19))
                .set(MEMBER.GENDER, "FEMALE")
                .set(MEMBER.CREATED_AT, OffsetDateTime.now())
                .set(MEMBER.UPDATED_AT, OffsetDateTime.now())
                .execute();

        dsl.insertInto(MEMBER)
                .set(MEMBER.ID, 3L)
                .set(MEMBER.NAME, "관리자")
                .set(MEMBER.STUDENT_NUMBER, "20210003")
                .set(MEMBER.MAJOR, "컴퓨터공학과")
                .set(MEMBER.ROLE, "STAFF")
                .set(MEMBER.GRADE, "SENIOR")
                .set(MEMBER.BIRTHDAY, OffsetDateTime.now().minusYears(22))
                .set(MEMBER.GENDER, "MALE")
                .set(MEMBER.CREATED_AT, OffsetDateTime.now())
                .set(MEMBER.UPDATED_AT, OffsetDateTime.now())
                .execute();

        // 상수 설정
        CREATOR_ID = 1L;
        PARTICIPANT_ID = 2L;
        ADMIN_ID = 3L;

        // 스터디 데이터 생성
        dsl.insertInto(STUDY)
                .set(STUDY.ID, TEST_STUDY_ID)
                .set(STUDY.TITLE, "테스트 스터디")
                .set(STUDY.DESCRIPTION, "테스트용 스터디입니다")
                .set(STUDY.CONTENT, "테스트 내용")
                .set(STUDY.CATEGORY, "CTF")
                .set(STUDY.SUBCATEGORY, "포너블")
                .set(STUDY.MEMBER_ID, CREATOR_ID)
                .set(STUDY.MAX_PARTICIPANTS_NUMBER, 10)
                .set(STUDY.STARTED_AT, OffsetDateTime.now().plusDays(7))
                .set(STUDY.ENDED_AT, OffsetDateTime.now().plusDays(35))
                .set(STUDY.CREATED_AT, OffsetDateTime.now())
                .set(STUDY.UPDATED_AT, OffsetDateTime.now())
                .execute();

        // 프로젝트 데이터 생성
        dsl.insertInto(PROJECT)
                .set(PROJECT.ID, TEST_PROJECT_ID)
                .set(PROJECT.TITLE, "테스트 프로젝트")
                .set(PROJECT.DESCRIPTION, "테스트용 프로젝트입니다")
                .set(PROJECT.CONTENT, "테스트 내용")
                .set(PROJECT.CATEGORY, "웹개발")
                .set(PROJECT.SUBCATEGORY, "풀스택")
                .set(PROJECT.MEMBER_ID, CREATOR_ID)
                .set(PROJECT.MAX_PARTICIPANTS_NUMBER, 5)
                .set(PROJECT.STARTED_AT, OffsetDateTime.now().plusDays(7))
                .set(PROJECT.ENDED_AT, OffsetDateTime.now().plusDays(35))
                .set(PROJECT.CREATED_AT, OffsetDateTime.now())
                .set(PROJECT.UPDATED_AT, OffsetDateTime.now())
                .execute();
    }

    /**
     * 테스트 데이터 정리
     */
    private void cleanupTestData() {
        dsl.deleteFrom(STUDY_PARTICIPANT).execute();
        dsl.deleteFrom(PROJECT_PARTICIPANT).execute();
        dsl.deleteFrom(STUDY).execute();
        dsl.deleteFrom(PROJECT).execute();
        dsl.deleteFrom(MEMBER).execute();
    }

    @Test
    @Order(1)
    @DisplayName("🔄 스터디 참가 신청 → 생성자 거절 → 다시 신청 (성공)")
    void testStudyRejectReapplyByCreator() throws Exception {
        // Given: 참가자가 스터디 참가 신청
        StudyJoinRequestDto joinRequest = new StudyJoinRequestDto();
        joinRequest.setStudyId(TEST_STUDY_ID);

        // 참가자로 보안 컨텍스트 설정
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new CurrentUser(PARTICIPANT_ID, "participant", "participant@certis.org", "참가자", "UPSOLVER"), 
                        null)
        );

        // When: 첫 번째 참가 신청
        mockMvc.perform(post("/api/v1/study/participant/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("스터디 참가 신청이 성공했습니다"));

        // 참가자 정보 확인
        var participant = dsl.selectFrom(STUDY_PARTICIPANT)
                .where(STUDY_PARTICIPANT.STUDY_ID.eq(TEST_STUDY_ID))
                .and(STUDY_PARTICIPANT.MEMBER_ID.eq(PARTICIPANT_ID))
                .and(STUDY_PARTICIPANT.DELETED_AT.isNull())
                .fetchOne();

        assertThat(participant).isNotNull();
        assertThat(participant.getStatus()).isEqualTo(StudyParticipantStatus.PENDING.name());

        Long participantId = participant.getId();

        // 생성자로 보안 컨텍스트 변경
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new CurrentUser(CREATOR_ID, "creator", "creator@certis.org", "생성자", "UPSOLVER"), 
                        null)
        );

        // When: 생성자가 참가 신청 거절
        StudyJoinRejectRequestDto rejectRequest = new StudyJoinRejectRequestDto();
        rejectRequest.setStudyId(TEST_STUDY_ID);
        rejectRequest.setMemberId(PARTICIPANT_ID);

        mockMvc.perform(post("/api/v1/study/participant/join/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("스터디 참가가 거절되었습니다"))
                .andExpect(jsonPath("$.data.currentStatus").value("REJECTED"));

        // 거절된 참가자가 소프트 삭제되었는지 확인
        var rejectedParticipant = dsl.selectFrom(STUDY_PARTICIPANT)
                .where(STUDY_PARTICIPANT.ID.eq(participantId))
                .fetchOne();

        assertThat(rejectedParticipant).isNotNull();
        assertThat(rejectedParticipant.getDeletedAt()).isNotNull(); // 소프트 삭제 확인

        // 참가자로 보안 컨텍스트 다시 설정
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new CurrentUser(PARTICIPANT_ID, "participant", "participant@certis.org", "참가자", "UPSOLVER"), 
                        null)
        );

        // When: 다시 참가 신청 (중복 체크 우회되어야 함)
        mockMvc.perform(post("/api/v1/study/participant/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("스터디 참가 신청이 성공했습니다"));

        // 새로운 참가 신청이 생성되었는지 확인
        var newParticipant = dsl.selectFrom(STUDY_PARTICIPANT)
                .where(STUDY_PARTICIPANT.STUDY_ID.eq(TEST_STUDY_ID))
                .and(STUDY_PARTICIPANT.MEMBER_ID.eq(PARTICIPANT_ID))
                .and(STUDY_PARTICIPANT.DELETED_AT.isNull())
                .fetchOne();

        assertThat(newParticipant).isNotNull();
        assertThat(newParticipant.getId()).isNotEqualTo(participantId); // 새로운 ID
        assertThat(newParticipant.getStatus()).isEqualTo(StudyParticipantStatus.PENDING.name());
    }

    @Test
    @Order(2)
    @DisplayName("🔄 프로젝트 참가 신청 → 생성자 거절 → 다시 신청 (성공)")
    void testProjectRejectReapplyByCreator() throws Exception {
        // Given: 참가자가 프로젝트 참가 신청
        ProjectJoinRequestDto joinRequest = new ProjectJoinRequestDto();
        joinRequest.setProjectId(TEST_PROJECT_ID);

        // 참가자로 보안 컨텍스트 설정
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new CurrentUser(PARTICIPANT_ID, "participant", "participant@certis.org", "참가자", "UPSOLVER"), 
                        null)
        );

        // When: 첫 번째 참가 신청
        mockMvc.perform(post("/api/v1/project/participant/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("프로젝트 참가 신청이 성공했습니다"));

        // 참가자 정보 확인
        var participant = dsl.selectFrom(PROJECT_PARTICIPANT)
                .where(PROJECT_PARTICIPANT.PROJECT_ID.eq(TEST_PROJECT_ID))
                .and(PROJECT_PARTICIPANT.MEMBER_ID.eq(PARTICIPANT_ID))
                .and(PROJECT_PARTICIPANT.DELETED_AT.isNull())
                .fetchOne();

        assertThat(participant).isNotNull();
        assertThat(participant.getStatus()).isEqualTo(ProjectParticipantStatus.PENDING.name());

        Long participantId = participant.getId();

        // 생성자로 보안 컨텍스트 변경
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new CurrentUser(CREATOR_ID, "creator", "creator@certis.org", "생성자", "UPSOLVER"), 
                        null)
        );

        // When: 생성자가 참가 신청 거절
        ProjectJoinRejectRequestDto rejectRequest = new ProjectJoinRejectRequestDto();
        rejectRequest.setProjectId(TEST_PROJECT_ID);
        rejectRequest.setMemberId(PARTICIPANT_ID);

        mockMvc.perform(post("/api/v1/project/participant/join/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("프로젝트 참가가 거절되었습니다"))
                .andExpect(jsonPath("$.data.currentStatus").value("REJECTED"));

        // 거절된 참가자가 소프트 삭제되었는지 확인
        var rejectedParticipant = dsl.selectFrom(PROJECT_PARTICIPANT)
                .where(PROJECT_PARTICIPANT.ID.eq(participantId))
                .fetchOne();

        assertThat(rejectedParticipant).isNotNull();
        assertThat(rejectedParticipant.getDeletedAt()).isNotNull(); // 소프트 삭제 확인

        // 참가자로 보안 컨텍스트 다시 설정
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new CurrentUser(PARTICIPANT_ID, "participant", "participant@certis.org", "참가자", "UPSOLVER"), 
                        null)
        );

        // When: 다시 참가 신청 (중복 체크 우회되어야 함)
        mockMvc.perform(post("/api/v1/project/participant/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("프로젝트 참가 신청이 성공했습니다"));

        // 새로운 참가 신청이 생성되었는지 확인
        var newParticipant = dsl.selectFrom(PROJECT_PARTICIPANT)
                .where(PROJECT_PARTICIPANT.PROJECT_ID.eq(TEST_PROJECT_ID))
                .and(PROJECT_PARTICIPANT.MEMBER_ID.eq(PARTICIPANT_ID))
                .and(PROJECT_PARTICIPANT.DELETED_AT.isNull())
                .fetchOne();

        assertThat(newParticipant).isNotNull();
        assertThat(newParticipant.getId()).isNotEqualTo(participantId); // 새로운 ID
        assertThat(newParticipant.getStatus()).isEqualTo(ProjectParticipantStatus.PENDING.name());
    }

    @Test
    @Order(3)
    @DisplayName("🔄 스터디 참가 신청 → 관리자 거절 → 다시 신청 (성공)")
    void testStudyRejectReapplyByAdmin() throws Exception {
        // Given: 참가자가 스터디 참가 신청
        StudyJoinRequestDto joinRequest = new StudyJoinRequestDto();
        joinRequest.setStudyId(TEST_STUDY_ID);

        // 참가자로 보안 컨텍스트 설정
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new CurrentUser(PARTICIPANT_ID, "participant", "participant@certis.org", "참가자", "UPSOLVER"), 
                        null)
        );

        // When: 첫 번째 참가 신청
        mockMvc.perform(post("/api/v1/study/participant/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("스터디 참가 신청이 성공했습니다"));

        // 참가자 정보 확인
        var participant = dsl.selectFrom(STUDY_PARTICIPANT)
                .where(STUDY_PARTICIPANT.STUDY_ID.eq(TEST_STUDY_ID))
                .and(STUDY_PARTICIPANT.MEMBER_ID.eq(PARTICIPANT_ID))
                .and(STUDY_PARTICIPANT.DELETED_AT.isNull())
                .fetchOne();

        assertThat(participant).isNotNull();
        assertThat(participant.getStatus()).isEqualTo(StudyParticipantStatus.PENDING.name());

        Long participantId = participant.getId();

        // 관리자로 보안 컨텍스트 변경
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new CurrentUser(ADMIN_ID, "admin", "admin@certis.org", "관리자", "STAFF"), 
                        null)
        );

        // When: 관리자가 참가 신청 거절
        StudyJoinRejectRequestDto rejectRequest = new StudyJoinRejectRequestDto();
        rejectRequest.setStudyId(TEST_STUDY_ID);
        rejectRequest.setMemberId(PARTICIPANT_ID);

        mockMvc.perform(post("/api/v1/study/participant/join/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("스터디 참가가 거절되었습니다"))
                .andExpect(jsonPath("$.data.currentStatus").value("REJECTED"));

        // 거절된 참가자가 소프트 삭제되었는지 확인
        var rejectedParticipant = dsl.selectFrom(STUDY_PARTICIPANT)
                .where(STUDY_PARTICIPANT.ID.eq(participantId))
                .fetchOne();

        assertThat(rejectedParticipant).isNotNull();
        assertThat(rejectedParticipant.getDeletedAt()).isNotNull(); // 소프트 삭제 확인

        // 참가자로 보안 컨텍스트 다시 설정
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new CurrentUser(PARTICIPANT_ID, "participant", "participant@certis.org", "참가자", "UPSOLVER"), 
                        null)
        );

        // When: 다시 참가 신청 (중복 체크 우회되어야 함)
        mockMvc.perform(post("/api/v1/study/participant/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("스터디 참가 신청이 성공했습니다"));

        // 새로운 참가 신청이 생성되었는지 확인
        var newParticipant = dsl.selectFrom(STUDY_PARTICIPANT)
                .where(STUDY_PARTICIPANT.STUDY_ID.eq(TEST_STUDY_ID))
                .and(STUDY_PARTICIPANT.MEMBER_ID.eq(PARTICIPANT_ID))
                .and(STUDY_PARTICIPANT.DELETED_AT.isNull())
                .fetchOne();

        assertThat(newParticipant).isNotNull();
        assertThat(newParticipant.getId()).isNotEqualTo(participantId); // 새로운 ID
        assertThat(newParticipant.getStatus()).isEqualTo(StudyParticipantStatus.PENDING.name());
    }

    @Test
    @Order(4)
    @DisplayName("🔄 프로젝트 참가 신청 → 관리자 거절 → 다시 신청 (성공)")
    void testProjectRejectReapplyByAdmin() throws Exception {
        // Given: 참가자가 프로젝트 참가 신청
        ProjectJoinRequestDto joinRequest = new ProjectJoinRequestDto();
        joinRequest.setProjectId(TEST_PROJECT_ID);

        // 참가자로 보안 컨텍스트 설정
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new CurrentUser(PARTICIPANT_ID, "participant", "participant@certis.org", "참가자", "UPSOLVER"), 
                        null)
        );

        // When: 첫 번째 참가 신청
        mockMvc.perform(post("/api/v1/project/participant/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("프로젝트 참가 신청이 성공했습니다"));

        // 참가자 정보 확인
        var participant = dsl.selectFrom(PROJECT_PARTICIPANT)
                .where(PROJECT_PARTICIPANT.PROJECT_ID.eq(TEST_PROJECT_ID))
                .and(PROJECT_PARTICIPANT.MEMBER_ID.eq(PARTICIPANT_ID))
                .and(PROJECT_PARTICIPANT.DELETED_AT.isNull())
                .fetchOne();

        assertThat(participant).isNotNull();
        assertThat(participant.getStatus()).isEqualTo(ProjectParticipantStatus.PENDING.name());

        Long participantId = participant.getId();

        // 관리자로 보안 컨텍스트 변경
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new CurrentUser(ADMIN_ID, "admin", "admin@certis.org", "관리자", "STAFF"), 
                        null)
        );

        // When: 관리자가 참가 신청 거절
        ProjectJoinRejectRequestDto rejectRequest = new ProjectJoinRejectRequestDto();
        rejectRequest.setProjectId(TEST_PROJECT_ID);
        rejectRequest.setMemberId(PARTICIPANT_ID);

        mockMvc.perform(post("/api/v1/project/participant/join/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("프로젝트 참가가 거절되었습니다"))
                .andExpect(jsonPath("$.data.currentStatus").value("REJECTED"));

        // 거절된 참가자가 소프트 삭제되었는지 확인
        var rejectedParticipant = dsl.selectFrom(PROJECT_PARTICIPANT)
                .where(PROJECT_PARTICIPANT.ID.eq(participantId))
                .fetchOne();

        assertThat(rejectedParticipant).isNotNull();
        assertThat(rejectedParticipant.getDeletedAt()).isNotNull(); // 소프트 삭제 확인

        // 참가자로 보안 컨텍스트 다시 설정
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new CurrentUser(PARTICIPANT_ID, "participant", "participant@certis.org", "참가자", "UPSOLVER"), 
                        null)
        );

        // When: 다시 참가 신청 (중복 체크 우회되어야 함)
        mockMvc.perform(post("/api/v1/project/participant/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("프로젝트 참가 신청이 성공했습니다"));

        // 새로운 참가 신청이 생성되었는지 확인
        var newParticipant = dsl.selectFrom(PROJECT_PARTICIPANT)
                .where(PROJECT_PARTICIPANT.PROJECT_ID.eq(TEST_PROJECT_ID))
                .and(PROJECT_PARTICIPANT.MEMBER_ID.eq(PARTICIPANT_ID))
                .and(PROJECT_PARTICIPANT.DELETED_AT.isNull())
                .fetchOne();

        assertThat(newParticipant).isNotNull();
        assertThat(newParticipant.getId()).isNotEqualTo(participantId); // 새로운 ID
        assertThat(newParticipant.getStatus()).isEqualTo(ProjectParticipantStatus.PENDING.name());
    }
}
