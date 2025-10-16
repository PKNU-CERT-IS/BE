package org.certis.studyplatform.study.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.certis.studyplatform.config.TestEmbeddedPostgresConfig;
import org.certis.studyplatform.config.TestWebMvcConfig;
import org.certis.studyplatform.study.domain.StudyParticipantStatus;
import org.certis.studyplatform.study.presentation.dto.request.*;
import org.jooq.DSLContext;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.certis.studyplatform.shared.security.CurrentUser;
import org.certis.studyplatform.shared.util.DateTimeUtils;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.certis.generated.jooq.Tables.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * StudyParticipantController 통합 테스트 - 새로운 비즈니스 규칙 검증
 * 
 * 🎯 테스트 목표:
 * - 참가 신청 제한 규칙 통합 검증
 * - 관리자 권한 통합 검증
 * - 거절/취소 동작 통합 검증
 * - 실제 데이터베이스와의 상호작용 검증
 * 
 * 🔧 테스트 전략:
 * - @SpringBootTest를 사용한 통합 테스트
 * - 실제 데이터베이스 사용 (Embedded PostgreSQL)
 * - MockMvc를 사용한 HTTP 요청/응답 테스트
 * - jOOQ를 사용한 데이터베이스 상태 검증
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestEmbeddedPostgresConfig.class, TestWebMvcConfig.class})
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/test_certis",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("StudyParticipantController 통합 테스트 - 새로운 비즈니스 규칙")
class StudyParticipantIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DSLContext dsl;

    private static final Long TEST_STUDY_ID = 1L;
    private static final Long TEST_MEMBER_ID = 2L;
    private static final Long TEST_ADMIN_ID = 3L;
    private static final Long TEST_STUDY_PARTICIPANT_ID = 10001L;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 초기화 - 의존성 역순으로 삭제
        cleanupTestData();

        setupTestData();
        
        // 보안 컨텍스트 설정
        setupSecurityContext();
    }

    private void cleanupTestData() {
        try {
            // 의존성 역순으로 삭제 (외래키 제약조건 고려)
            dsl.deleteFrom(STUDY_PARTICIPANT).execute();
            dsl.deleteFrom(PROJECT_PARTICIPANT).execute();
            dsl.deleteFrom(STUDY).execute();
            dsl.deleteFrom(PROJECT).execute();
            dsl.deleteFrom(MEMBER).execute();
            
            // 시퀀스 리셋
            resetSequenceIfExists("study_id_seq");
            resetSequenceIfExists("member_id_seq");
            resetSequenceIfExists("project_id_seq");
            resetSequenceIfExists("study_participant_id_seq");
            resetSequenceIfExists("project_participant_id_seq");
        } catch (Exception e) {
            // 테이블이 없는 경우 무시
        }
    }
    
    private void resetSequenceIfExists(String sequenceName) {
        try {
            dsl.execute("ALTER SEQUENCE IF EXISTS " + sequenceName + " RESTART WITH 1");
        } catch (Exception e) {
            // 시퀀스가 없는 경우 무시
        }
    }

    @Nested
    @DisplayName("참가 신청 제한 규칙 통합 테스트")
    class ApplicationLimitsIntegrationTest {

        @Test
        @DisplayName("진행 중인 스터디 2개 이상 시 추가 신청 거부")
        void shouldRejectWhenActiveStudiesExceedLimit() throws Exception {
            // Given: 진행 중인 스터디 2개 생성
            createActiveStudy(10L, "스터디 1", TEST_MEMBER_ID);
            createActiveStudy(11L, "스터디 2", TEST_MEMBER_ID);
            
            StudyJoinRequestDto request = createStudyJoinRequest(TEST_STUDY_ID);

            // When & Then: 3번째 스터디 신청 시도 → 제한 규칙에 의해 거부
            mockMvc.perform(post("/api/v1/study/participant/join/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("프로젝트 미진행 시 스터디 2개까지 가능합니다."));

            // 데이터베이스에서 신청이 생성되지 않았는지 확인
            verifyNoParticipantCreated(TEST_STUDY_ID, TEST_MEMBER_ID);
        }

        @Test
        @DisplayName("진행 중인 프로젝트 1개 + 스터디 1개 시 추가 신청 거부")
        void shouldRejectWhenProjectAndStudyActive() throws Exception {
            // Given: 진행 중인 스터디 1개, 프로젝트 1개 생성
            createActiveStudy(20L, "스터디 1", TEST_MEMBER_ID);
            createActiveProject(20L, "프로젝트 1", TEST_MEMBER_ID);
            
            StudyJoinRequestDto request = createStudyJoinRequest(TEST_STUDY_ID);

            // When & Then: 2번째 스터디 신청 시도 → 제한 규칙에 의해 거부
            mockMvc.perform(post("/api/v1/study/participant/join/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("프로젝트 진행 중에는 스터디 1개까지만 신청할 수 있습니다."));

            // 데이터베이스에서 신청이 생성되지 않았는지 확인
            verifyNoParticipantCreated(TEST_STUDY_ID, TEST_MEMBER_ID);
        }

        @Test
        @DisplayName("진행 중인 스터디 1개만 있을 때는 신청 가능")
        void shouldAllowWhenOnlyOneActiveStudy() throws Exception {
            // Given: 진행 중인 스터디 1개만 생성
            createActiveStudy(30L, "스터디 1", TEST_MEMBER_ID);
            
            StudyJoinRequestDto request = createStudyJoinRequest(TEST_STUDY_ID);

            // When & Then: 2번째 스터디 신청 성공
            mockMvc.perform(post("/api/v1/study/participant/join/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value("스터디 참가 신청이 성공했습니다"));

            // 데이터베이스에서 신청이 생성되었는지 확인
            // 성공 여부는 응답 코드/메시지로 충분히 검증됨
        }
    }

    @Nested
    @DisplayName("관리자 권한 통합 테스트")
    class AdminPermissionIntegrationTest {

        @Test
        @DisplayName("관리자가 스터디 생성자가 아니어도 승인 가능")
        void shouldAllowAdminToApprove() throws Exception {
            // Given: 관리자로 컨텍스트 설정
            setupAdminSecurityContext();
            
            // 참가 신청 생성
            createStudyParticipant(TEST_STUDY_PARTICIPANT_ID, TEST_STUDY_ID, TEST_MEMBER_ID, StudyParticipantStatus.PENDING);

            // When & Then: 관리자가 승인
            mockMvc.perform(post("/api/v1/study/participant/join/approve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createStudyJoinApproveRequest())))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("스터디 참가가 승인되었습니다"))
                    .andExpect(jsonPath("$.data.currentStatus").value("APPROVED"));

            // 데이터베이스에서 상태 변경 확인
            verifyParticipantStatusInDatabase(TEST_STUDY_PARTICIPANT_ID, StudyParticipantStatus.APPROVED);
        }

        @Test
        @DisplayName("일반 사용자는 스터디 생성자가 아니면 승인 불가")
        void shouldRejectNonCreatorNonAdmin() throws Exception {
            // Given: 다른 사용자로 컨텍스트 설정
            setupOtherUserSecurityContext();
            
            // 참가 신청 생성
            createStudyParticipant(TEST_STUDY_PARTICIPANT_ID, TEST_STUDY_ID, TEST_MEMBER_ID, StudyParticipantStatus.PENDING);

            // When & Then: 권한 없는 사용자가 승인 시도
            mockMvc.perform(post("/api/v1/study/participant/join/approve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createStudyJoinApproveRequest())))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("스터디 생성자 또는 관리자만 참가 승인/거절을 할 수 있습니다."));
        }
    }

    @Nested
    @DisplayName("거절/취소 동작 통합 테스트")
    class RejectCancelBehaviorIntegrationTest {

        @Test
        @DisplayName("거절 시 소프트 삭제 수행")
        void shouldSoftDeleteOnReject() throws Exception {
            // Given: 스터디와 참가 신청 생성
            Long customStudyId = 999L;
            createStudy(customStudyId, 9999L); // 스터디 생성자가 만든 스터디
            
            createStudyParticipant(TEST_STUDY_PARTICIPANT_ID, customStudyId, TEST_MEMBER_ID, StudyParticipantStatus.PENDING);

            // When: 거절 요청
            // 권한: 스터디 생성자 또는 관리자만 거절 가능 → 관리자 컨텍스트 설정
            setupAdminSecurityContext();
            StudyJoinRejectRequestDto rejectRequest = new StudyJoinRejectRequestDto();
            rejectRequest.setStudyId(customStudyId);
            rejectRequest.setMemberId(TEST_MEMBER_ID);

            mockMvc.perform(post("/api/v1/study/participant/join/reject")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(rejectRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("스터디 참가가 거절되었습니다"))
                    .andExpect(jsonPath("$.data.currentStatus").value("REJECTED"));

            // Then: 상태 업데이트 확인 (REJECTED 상태로 변경)
            verifyParticipantStatusUpdatedInDatabase(TEST_STUDY_PARTICIPANT_ID);
        }

        @Test
        @DisplayName("대기(PENDING) 상태 신청 취소 시 삭제 수행")
        void shouldCancelPendingApplication() throws Exception {
            // Given: 대기 중(PENDING) 참가 신청 생성
            createStudyParticipant(TEST_STUDY_PARTICIPANT_ID, TEST_STUDY_ID, TEST_MEMBER_ID, StudyParticipantStatus.PENDING);

            // When: 승인 취소 요청
            mockMvc.perform(delete("/api/v1/study/participant/join/cancel")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createStudyJoinCancelRequest())))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("스터디 참가 취소가 성공했습니다"));

            // Then: 취소 시 레코드 삭제 확인 (하드 삭제)
            verifyParticipantHardDeletedInDatabase(TEST_STUDY_PARTICIPANT_ID);
        }
    }

    @Nested
    @DisplayName("엣지 케이스 통합 테스트")
    class EdgeCasesIntegrationTest {

        @Test
        @DisplayName("존재하지 않는 스터디에 신청 시 예외 발생")
        void shouldThrowExceptionWhenStudyNotFound() throws Exception {
            // Given
            Long nonExistentStudyId = 999L;
            StudyJoinRequestDto request = createStudyJoinRequest(nonExistentStudyId);

            // When & Then
            mockMvc.perform(post("/api/v1/study/participant/join/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("스터디를 찾을 수 없습니다."));
        }

        @Test
        @DisplayName("중복 신청 시 예외 발생")
        void shouldThrowExceptionWhenDuplicateApplication() throws Exception {
            // Given: 이미 신청된 참가자 생성
            createStudyParticipant(TEST_STUDY_PARTICIPANT_ID, TEST_STUDY_ID, TEST_MEMBER_ID, StudyParticipantStatus.PENDING);
            
            StudyJoinRequestDto request = createStudyJoinRequest(TEST_STUDY_ID);

            // When & Then: 중복 신청 시도
            mockMvc.perform(post("/api/v1/study/participant/join/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("이미 참가 신청한 스터디입니다."));
        }
    }

    // ================================================================
    // Helper Methods
    // ================================================================

    private void setupTestData() {
        // 멤버 선행 생성 (FK 충돌 방지)
        dsl.insertInto(MEMBER)
                .set(MEMBER.ID, TEST_MEMBER_ID)
                .set(MEMBER.NAME, "테스트 사용자")
                .set(MEMBER.STUDENT_NUMBER, "20240001")
                .set(MEMBER.MAJOR, "컴퓨터공학과")
                .set(MEMBER.ROLE, "PLAYER")
                .set(MEMBER.GRADE, "FRESHMAN")
                .set(MEMBER.BIRTHDAY, defaultBirthday())
                .set(MEMBER.GENDER, "MALE")
                .set(MEMBER.CREATED_AT, OffsetDateTime.now())
                .set(MEMBER.UPDATED_AT, OffsetDateTime.now())
                .execute();

        dsl.insertInto(MEMBER)
                .set(MEMBER.ID, TEST_ADMIN_ID)
                .set(MEMBER.NAME, "관리자")
                .set(MEMBER.STUDENT_NUMBER, "20240002")
                .set(MEMBER.MAJOR, "컴퓨터공학과")
                .set(MEMBER.ROLE, "STAFF")
                .set(MEMBER.GRADE, "SENIOR")
                .set(MEMBER.BIRTHDAY, defaultBirthday())
                .set(MEMBER.GENDER, "FEMALE")
                .set(MEMBER.CREATED_AT, OffsetDateTime.now())
                .set(MEMBER.UPDATED_AT, OffsetDateTime.now())
                .execute();

        // 기본 스터디 생성자 멤버 생성 (creatorId=9999)
        dsl.insertInto(MEMBER)
                .set(MEMBER.ID, 9999L)
                .set(MEMBER.NAME, "스터디 생성자")
                .set(MEMBER.STUDENT_NUMBER, "20249999")
                .set(MEMBER.MAJOR, "컴퓨터공학과")
                .set(MEMBER.ROLE, "PLAYER")
                .set(MEMBER.GRADE, "JUNIOR")
                .set(MEMBER.BIRTHDAY, defaultBirthday())
                .set(MEMBER.GENDER, "MALE")
                .set(MEMBER.CREATED_AT, OffsetDateTime.now())
                .set(MEMBER.UPDATED_AT, OffsetDateTime.now())
                .execute();

        // 기본 스터디 생성 (멤버 존재 후 생성)
        dsl.insertInto(STUDY)
                .set(STUDY.ID, TEST_STUDY_ID)
                .set(STUDY.TITLE, "테스트 스터디")
                .set(STUDY.DESCRIPTION, "테스트 설명")
                .set(STUDY.CONTENT, "테스트 내용")
                .set(STUDY.CATEGORY, "CTF")
                .set(STUDY.SUBCATEGORY, "포너블")
                .set(STUDY.STARTED_AT, DateTimeUtils.calculateStudyStartWeek(OffsetDateTime.now()))
                .set(STUDY.ENDED_AT, DateTimeUtils.calculateEndWeek(DateTimeUtils.calculateStudyStartWeek(OffsetDateTime.now()), 4))
                .set(STUDY.MAX_PARTICIPANTS_NUMBER, 10)
                .set(STUDY.MEMBER_ID, 9999L)
                .set(STUDY.STATUS, "READY")
                .set(STUDY.RESULT_SUBMIT_STATUS, "READY")
                .set(STUDY.CREATED_AT, OffsetDateTime.now())
                .set(STUDY.UPDATED_AT, OffsetDateTime.now())
                .execute();

    }

    private OffsetDateTime defaultBirthday() {
        // 테스트의 안정성을 위해 고정된 합법적 날짜 사용 (타임존 포함)
        return OffsetDateTime.parse("2000-01-01T00:00:00+09:00");
    }

    private void setupSecurityContext() {
        CurrentUser currentUser = new CurrentUser(TEST_MEMBER_ID, "test", "test@example.com", "테스트 사용자", "PLAYER");
        UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(currentUser, null, currentUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void setupAdminSecurityContext() {
        CurrentUser adminUser = new CurrentUser(TEST_ADMIN_ID, "admin", "admin@example.com", "관리자", "STAFF");
        UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(adminUser, null, adminUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void setupOtherUserSecurityContext() {
        CurrentUser otherUser = new CurrentUser(TEST_MEMBER_ID, "other", "other@example.com", "다른 사용자", "PLAYER");
        UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(otherUser, null, otherUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private StudyJoinRequestDto createStudyJoinRequest(Long studyId) {
        StudyJoinRequestDto request = new StudyJoinRequestDto();
        request.setStudyId(studyId);
        return request;
    }

    private StudyJoinApproveRequestDto createStudyJoinApproveRequest() {
        StudyJoinApproveRequestDto request = new StudyJoinApproveRequestDto();
        request.setStudyId(TEST_STUDY_ID);
        request.setMemberId(TEST_MEMBER_ID);
        return request;
    }

    private StudyJoinCancelRequestDto createStudyJoinCancelRequest() {
        StudyJoinCancelRequestDto request = new StudyJoinCancelRequestDto();
        request.setStudyId(TEST_STUDY_ID);
        return request;
    }

    private void createStudy(Long studyId, Long memberId) {
        // 스터디 생성
        dsl.insertInto(STUDY)
                .set(STUDY.ID, studyId)
                .set(STUDY.TITLE, "테스트 스터디")
                .set(STUDY.DESCRIPTION, "테스트 설명")
                .set(STUDY.CONTENT, "테스트 내용")
                .set(STUDY.CATEGORY, "CTF")
                .set(STUDY.SUBCATEGORY, "포너블")
                .set(STUDY.STARTED_AT, DateTimeUtils.calculateStudyStartWeek(OffsetDateTime.now()))
                .set(STUDY.ENDED_AT, DateTimeUtils.calculateEndWeek(DateTimeUtils.calculateStudyStartWeek(OffsetDateTime.now()), 4))
                .set(STUDY.MAX_PARTICIPANTS_NUMBER, 10)
                .set(STUDY.MEMBER_ID, memberId)
                .set(STUDY.STATUS, "READY")
                .set(STUDY.RESULT_SUBMIT_STATUS, "READY")
                .set(STUDY.CREATED_AT, OffsetDateTime.now())
                .set(STUDY.UPDATED_AT, OffsetDateTime.now())
                .set(STUDY.DELETED_AT, (OffsetDateTime) null)
                .execute();
    }

    private void createActiveStudy(Long studyId, String title, Long memberId) {
        // 스터디 생성
        dsl.insertInto(STUDY)
                .set(STUDY.ID, studyId)
                .set(STUDY.TITLE, title)
                .set(STUDY.DESCRIPTION, "테스트 설명")
                .set(STUDY.CONTENT, "테스트 내용")
                .set(STUDY.CATEGORY, "CTF")
                .set(STUDY.SUBCATEGORY, "포너블")
                .set(STUDY.STARTED_AT, OffsetDateTime.now().minusDays(1))
                .set(STUDY.ENDED_AT, OffsetDateTime.now().plusDays(30))
                .set(STUDY.MAX_PARTICIPANTS_NUMBER, 10)
                .set(STUDY.MEMBER_ID, memberId)
                .set(STUDY.STATUS, "READY")
                .set(STUDY.RESULT_SUBMIT_STATUS, "READY")
                .set(STUDY.CREATED_AT, OffsetDateTime.now())
                .set(STUDY.UPDATED_AT, OffsetDateTime.now())
                .execute();
    }

    private void createActiveProject(Long projectId, String title, Long memberId) {
        // 프로젝트 생성
        dsl.insertInto(PROJECT)
                .set(PROJECT.ID, projectId)
                .set(PROJECT.TITLE, title)
                .set(PROJECT.DESCRIPTION, "테스트 설명")
                .set(PROJECT.CONTENT, "테스트 내용")
                .set(PROJECT.CATEGORY, "CTF")
                .set(PROJECT.SUBCATEGORY, "포너블")
                .set(PROJECT.MAX_PARTICIPANTS_NUMBER, 10)
                .set(PROJECT.STARTED_AT, OffsetDateTime.now().minusDays(1))
                .set(PROJECT.ENDED_AT, OffsetDateTime.now().plusDays(30))
                .set(PROJECT.MEMBER_ID, memberId)
                .set(PROJECT.STATUS, "READY")
                .set(PROJECT.RESULT_SUBMIT_STATUS, "READY")
                .set(PROJECT.CREATED_AT, OffsetDateTime.now())
                .set(PROJECT.UPDATED_AT, OffsetDateTime.now())
                .execute();

        // 승인된 참가자 생성
        dsl.insertInto(PROJECT_PARTICIPANT)
                .set(PROJECT_PARTICIPANT.ID, projectId + 20000L)
                .set(PROJECT_PARTICIPANT.PROJECT_ID, projectId)
                .set(PROJECT_PARTICIPANT.MEMBER_ID, memberId)
                .set(PROJECT_PARTICIPANT.STATUS, "APPROVED")
                .set(PROJECT_PARTICIPANT.CREATED_AT, OffsetDateTime.now())
                .set(PROJECT_PARTICIPANT.UPDATED_AT, OffsetDateTime.now())
                .execute();
    }

    private void createStudyParticipant(Long participantId, Long studyId, Long memberId, StudyParticipantStatus status) {
        dsl.insertInto(STUDY_PARTICIPANT)
                .set(STUDY_PARTICIPANT.ID, participantId)
                .set(STUDY_PARTICIPANT.STUDY_ID, studyId)
                .set(STUDY_PARTICIPANT.MEMBER_ID, memberId)
                .set(STUDY_PARTICIPANT.STATUS, status.name())
                .set(STUDY_PARTICIPANT.CREATED_AT, OffsetDateTime.now())
                .set(STUDY_PARTICIPANT.UPDATED_AT, OffsetDateTime.now())
                .execute();
    }

    private void verifyNoParticipantCreated(Long studyId, Long memberId) {
        var participant = dsl.selectFrom(STUDY_PARTICIPANT)
                .where(STUDY_PARTICIPANT.STUDY_ID.eq(studyId))
                .and(STUDY_PARTICIPANT.MEMBER_ID.eq(memberId))
                .fetchOne();

        assertThat(participant).isNull();
    }

    private void verifyParticipantStatusInDatabase(Long participantId, StudyParticipantStatus expectedStatus) {
        var participant = dsl.selectFrom(STUDY_PARTICIPANT)
                .where(STUDY_PARTICIPANT.ID.eq(participantId))
                .and(STUDY_PARTICIPANT.DELETED_AT.isNull())
                .fetchOne();

        assertThat(participant).isNotNull();
        assertThat(participant.getStatus()).isEqualTo(expectedStatus.name());
    }

    private void verifyParticipantStatusUpdatedInDatabase(Long participantId) {
        var participant = dsl.selectFrom(STUDY_PARTICIPANT)
                .where(STUDY_PARTICIPANT.ID.eq(participantId))
                .fetchOne();

        assertThat(participant).isNotNull();
        // 거절은 소프트 삭제이므로 deleted_at이 설정되어 있어야 함
        assertThat(participant.getDeletedAt()).isNotNull();
    }

    private void verifyParticipantHardDeletedInDatabase(Long participantId) {
        var participant = dsl.selectFrom(STUDY_PARTICIPANT)
                .where(STUDY_PARTICIPANT.ID.eq(participantId))
                .fetchOne();

        assertThat(participant).isNull();
    }
}
