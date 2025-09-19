package org.certis.studyplatform.project.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.certis.studyplatform.config.TestEmbeddedPostgresConfig;
import org.certis.studyplatform.config.TestWebMvcConfig;
import org.certis.studyplatform.project.domain.ProjectParticipantStatus;
import org.certis.studyplatform.project.presentation.dto.request.*;
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

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.certis.generated.jooq.Tables.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ProjectParticipantController 통합 테스트 - 새로운 비즈니스 규칙 검증
 * 
 * 🎯 테스트 목표:
 * - 프로젝트 참가 신청 제한 규칙 통합 검증 (진행 중인 프로젝트 1개 제한)
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
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("ProjectParticipantController 통합 테스트 - 새로운 비즈니스 규칙")
class ProjectParticipantIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DSLContext dsl;

    private static final Long TEST_PROJECT_ID = 1L;
    private static final Long TEST_MEMBER_ID = 2L;
    private static final Long TEST_ADMIN_ID = 3L;
    private static final Long TEST_PROJECT_PARTICIPANT_ID = 1L;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 초기화
        setupTestData();
        
        // 보안 컨텍스트 설정
        setupSecurityContext();
    }

    @Nested
    @DisplayName("참가 신청 제한 규칙 통합 테스트")
    class ApplicationLimitsIntegrationTest {

        @Test
        @DisplayName("진행 중인 프로젝트 1개 이상 시 추가 신청 거부")
        void shouldRejectWhenActiveProjectsExceedLimit() throws Exception {
            // Given: 진행 중인 프로젝트 1개 생성
            createActiveProject(100L, "프로젝트 1", TEST_MEMBER_ID);
            
            ProjectJoinRequestDto request = createProjectJoinRequest();
            request.setProjectId(TEST_PROJECT_ID);

            // When & Then: 2번째 프로젝트 신청 시도
            mockMvc.perform(post("/api/v1/project/participant/join/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("진행 중인 프로젝트가 1개 있으면 추가 신청이 불가합니다."));

            // 데이터베이스에서 신청이 생성되지 않았는지 확인
            verifyNoParticipantCreated(TEST_PROJECT_ID, TEST_MEMBER_ID);
        }

        @Test
        @DisplayName("진행 중인 프로젝트가 0개면 신청 가능")
        void shouldAllowWhenNoActiveProjects() throws Exception {
            // Given: 진행 중인 프로젝트 없음
            
            ProjectJoinRequestDto request = createProjectJoinRequest();
            request.setProjectId(TEST_PROJECT_ID);

            // When & Then: 첫 번째 프로젝트 신청 성공
            mockMvc.perform(post("/api/v1/project/participant/join/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value("프로젝트 참가 신청이 성공했습니다"));

            // 데이터베이스에서 신청이 생성되었는지 확인
            verifyParticipantCreated(TEST_PROJECT_ID, TEST_MEMBER_ID, ProjectParticipantStatus.PENDING);
        }

        @Test
        @DisplayName("프로젝트 생성자는 제한 규칙 적용 안됨")
        void shouldAllowCreatorToJoinWithoutLimit() throws Exception {
            // Given: 진행 중인 프로젝트 1개 생성 (다른 사용자)
            createActiveProject(2L, "다른 프로젝트", 999L);
            
            // 프로젝트 생성자로 컨텍스트 설정
            setupCreatorSecurityContext();
            
            ProjectJoinRequestDto request = createProjectJoinRequest();
            request.setProjectId(TEST_PROJECT_ID);

            // When & Then: 생성자가 자신의 프로젝트에 참가 신청 시도 (실제로는 자가 신청이므로 거부되어야 함)
            mockMvc.perform(post("/api/v1/project/participant/join/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value("프로젝트 생성자는 자신의 프로젝트에 참가 신청할 수 없습니다."));
        }
    }

    @Nested
    @DisplayName("관리자 권한 통합 테스트")
    class AdminPermissionIntegrationTest {

        @Test
        @DisplayName("관리자가 프로젝트 생성자가 아니어도 승인 가능")
        void shouldAllowAdminToApprove() throws Exception {
            // Given: 관리자로 컨텍스트 설정
            setupAdminSecurityContext();
            
            // 참가 신청 생성
            createProjectParticipant(TEST_PROJECT_PARTICIPANT_ID, TEST_PROJECT_ID, TEST_MEMBER_ID, ProjectParticipantStatus.PENDING);

            // When & Then: 관리자가 승인
            var approveReq = new ProjectJoinApproveRequestDto();
            approveReq.setParticipantId(TEST_PROJECT_PARTICIPANT_ID);
            mockMvc.perform(post("/api/v1/project/participant/join/approve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(approveReq)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("프로젝트 참가가 승인되었습니다"))
                    .andExpect(jsonPath("$.data.currentStatus").value("APPROVED"));

            // 데이터베이스에서 상태 변경 확인
            verifyParticipantStatusInDatabase(TEST_PROJECT_PARTICIPANT_ID, ProjectParticipantStatus.APPROVED);
        }

        @Test
        @DisplayName("일반 사용자는 프로젝트 생성자가 아니면 승인 불가")
        void shouldRejectNonCreatorNonAdmin() throws Exception {
            // Given: 다른 사용자로 컨텍스트 설정
            setupOtherUserSecurityContext();
            
            // 참가 신청 생성
            createProjectParticipant(TEST_PROJECT_PARTICIPANT_ID, TEST_PROJECT_ID, TEST_MEMBER_ID, ProjectParticipantStatus.PENDING);

            // When & Then: 권한 없는 사용자가 승인 시도
            var approveReq2 = new ProjectJoinApproveRequestDto();
            approveReq2.setParticipantId(TEST_PROJECT_PARTICIPANT_ID);
            mockMvc.perform(post("/api/v1/project/participant/join/approve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(approveReq2)))
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("프로젝트 생성자 또는 관리자만 참가 승인/거절을 할 수 있습니다."));
        }

        @Test
        @DisplayName("CHAIRMAN 권한도 승인 가능")
        void shouldAllowChairmanToApprove() throws Exception {
            // Given: CHAIRMAN으로 컨텍스트 설정
            setupChairmanSecurityContext();
            
            // 참가 신청 생성
            createProjectParticipant(TEST_PROJECT_PARTICIPANT_ID, TEST_PROJECT_ID, TEST_MEMBER_ID, ProjectParticipantStatus.PENDING);

            // When & Then: CHAIRMAN이 승인
            var approveReq3 = new ProjectJoinApproveRequestDto();
            approveReq3.setParticipantId(TEST_PROJECT_PARTICIPANT_ID);
            mockMvc.perform(post("/api/v1/project/participant/join/approve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(approveReq3)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("프로젝트 참가가 승인되었습니다"))
                    .andExpect(jsonPath("$.data.currentStatus").value("APPROVED"));

            // 데이터베이스에서 상태 변경 확인
            verifyParticipantStatusInDatabase(TEST_PROJECT_PARTICIPANT_ID, ProjectParticipantStatus.APPROVED);
        }
    }

    @Nested
    @DisplayName("거절/취소 동작 통합 테스트")
    class RejectCancelBehaviorIntegrationTest {

        @Test
        @DisplayName("거절 시 소프트 삭제 수행")
        void shouldSoftDeleteOnReject() throws Exception {
            // Given: 참가 신청 생성
            createProjectParticipant(TEST_PROJECT_PARTICIPANT_ID, TEST_PROJECT_ID, TEST_MEMBER_ID, ProjectParticipantStatus.PENDING);

            // 권한: 프로젝트 생성자 또는 STAFF 이상만 거절 가능 → 관리자 컨텍스트 설정
            setupAdminSecurityContext();

            // When: 거절 요청
            var rejectReq = new ProjectJoinRejectRequestDto();
            rejectReq.setParticipantId(TEST_PROJECT_PARTICIPANT_ID);
            mockMvc.perform(post("/api/v1/project/participant/join/reject")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(rejectReq)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("프로젝트 참가가 거절되었습니다"))
                    .andExpect(jsonPath("$.data.currentStatus").value("REJECTED"));

            // Then: 소프트 삭제 확인 (deleted_at 설정)
            verifyParticipantSoftDeletedInDatabase(TEST_PROJECT_PARTICIPANT_ID);
        }

        @Test
        @DisplayName("대기(PENDING) 상태 신청 취소 시 삭제 수행")
        void shouldCancelPendingApplication() throws Exception {
            // Given: 대기(PENDING) 참가 신청 생성
            createProjectParticipant(TEST_PROJECT_PARTICIPANT_ID, TEST_PROJECT_ID, TEST_MEMBER_ID, ProjectParticipantStatus.PENDING);

            // When: 참가 신청 취소 요청
            var cancelReqApproved = new ProjectJoinCancelRequestDto();
            cancelReqApproved.setProjectId(TEST_PROJECT_ID);
            mockMvc.perform(delete("/api/v1/project/participant/join/cancel")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cancelReqApproved)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("프로젝트 참가 취소가 성공했습니다"));

            // Then: 취소 시 레코드 삭제 확인 (하드 삭제)
            verifyParticipantHardDeletedInDatabase(TEST_PROJECT_PARTICIPANT_ID);
        }

        @Test
        @DisplayName("승인되지 않은 참가자 취소 시 예외 발생")
        void shouldThrowExceptionWhenCancellingNonApprovedParticipant() throws Exception {
            // Given: 대기 중인 참가자 생성
            createProjectParticipant(TEST_PROJECT_PARTICIPANT_ID, TEST_PROJECT_ID, TEST_MEMBER_ID, ProjectParticipantStatus.PENDING);

            // When & Then: 승인되지 않은 참가자 취소 시도
            var cancelReq = new ProjectJoinCancelRequestDto();
            cancelReq.setProjectId(TEST_PROJECT_ID);
            mockMvc.perform(delete("/api/v1/project/participant/join/cancel")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cancelReq)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("프로젝트 참가 취소가 성공했습니다"));
        }
    }

    @Nested
    @DisplayName("엣지 케이스 통합 테스트")
    class EdgeCasesIntegrationTest {

        @Test
        @DisplayName("존재하지 않는 프로젝트에 신청 시 예외 발생")
        void shouldThrowExceptionWhenProjectNotFound() throws Exception {
            // Given
            ProjectJoinRequestDto request = createProjectJoinRequest();
            Long nonExistentProjectId = 999L;

            // When & Then
            request.setProjectId(nonExistentProjectId);
            mockMvc.perform(post("/api/v1/project/participant/join/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("프로젝트를 찾을 수 없습니다."));
        }

        @Test
        @DisplayName("중복 신청 시 예외 발생")
        void shouldThrowExceptionWhenDuplicateApplication() throws Exception {
            // Given: 이미 신청된 참가자 생성
            createProjectParticipant(TEST_PROJECT_PARTICIPANT_ID, TEST_PROJECT_ID, TEST_MEMBER_ID, ProjectParticipantStatus.PENDING);
            
            ProjectJoinRequestDto request = createProjectJoinRequest();

            // When & Then: 중복 신청 시도
            mockMvc.perform(post("/api/v1/project/participant/join/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("이미 참가 신청한 프로젝트입니다."));
        }

        @Test
        @DisplayName("정원 초과 신청 시 예외 발생")
        void shouldThrowExceptionWhenExceedingMaxParticipants() throws Exception {
            // Given: 정원을 가득 채운 프로젝트
            dsl.update(PROJECT)
                    .set(PROJECT.MAX_PARTICIPANTS_NUMBER, 1)
                    .where(PROJECT.ID.eq(TEST_PROJECT_ID))
                    .execute();
            
            // 이미 1명이 참가한 상태
            createProjectParticipant(999L, TEST_PROJECT_ID, 999L, ProjectParticipantStatus.APPROVED);
            
            ProjectJoinRequestDto request = createProjectJoinRequest();

            // When & Then: 정원 초과 신청 시도
            mockMvc.perform(post("/api/v1/project/participant/join/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("프로젝트 정원이 가득 찼습니다."));
        }
    }

    // ================================================================
    // Helper Methods
    // ================================================================

    private void setupTestData() {
        // 멤버 선행 생성 (FK 제약조건 대비) - 생성자(1), 기본 사용자(2), 관리자(3), 기타 사용자(999)
        dsl.insertInto(MEMBER)
                .set(MEMBER.ID, 1L)
                .set(MEMBER.NAME, "프로젝트 생성자")
                .set(MEMBER.STUDENT_NUMBER, "20240000")
                .set(MEMBER.MAJOR, "컴퓨터공학과")
                .set(MEMBER.GRADE, "JUNIOR")
                .set(MEMBER.ROLE, "PLAYER")
                .set(MEMBER.BIRTHDAY, OffsetDateTime.now().minusYears(20))
                .set(MEMBER.GENDER, "MALE")
                .set(MEMBER.CREATED_AT, OffsetDateTime.now())
                .set(MEMBER.UPDATED_AT, OffsetDateTime.now())
                .execute();

        dsl.insertInto(MEMBER)
                .set(MEMBER.ID, TEST_MEMBER_ID)
                .set(MEMBER.NAME, "테스트 사용자")
                .set(MEMBER.STUDENT_NUMBER, "20240001")
                .set(MEMBER.MAJOR, "컴퓨터공학과")
                .set(MEMBER.GRADE, "SOPHOMORE")
                .set(MEMBER.ROLE, "PLAYER")
                .set(MEMBER.BIRTHDAY, OffsetDateTime.now().minusYears(20))
                .set(MEMBER.GENDER, "MALE")
                .set(MEMBER.CREATED_AT, OffsetDateTime.now())
                .set(MEMBER.UPDATED_AT, OffsetDateTime.now())
                .execute();

        dsl.insertInto(MEMBER)
                .set(MEMBER.ID, TEST_ADMIN_ID)
                .set(MEMBER.NAME, "관리자")
                .set(MEMBER.STUDENT_NUMBER, "20240002")
                .set(MEMBER.MAJOR, "컴퓨터공학과")
                .set(MEMBER.GRADE, "GRADUATED")
                .set(MEMBER.ROLE, "STAFF")
                .set(MEMBER.BIRTHDAY, OffsetDateTime.now().minusYears(25))
                .set(MEMBER.GENDER, "MALE")
                .set(MEMBER.CREATED_AT, OffsetDateTime.now())
                .set(MEMBER.UPDATED_AT, OffsetDateTime.now())
                .execute();

        dsl.insertInto(MEMBER)
                .set(MEMBER.ID, 999L)
                .set(MEMBER.NAME, "다른 사용자")
                .set(MEMBER.STUDENT_NUMBER, "20249999")
                .set(MEMBER.MAJOR, "컴퓨터공학과")
                .set(MEMBER.GRADE, "FRESHMAN")
                .set(MEMBER.ROLE, "PLAYER")
                .set(MEMBER.BIRTHDAY, OffsetDateTime.now().minusYears(22))
                .set(MEMBER.GENDER, "MALE")
                .set(MEMBER.CREATED_AT, OffsetDateTime.now())
                .set(MEMBER.UPDATED_AT, OffsetDateTime.now())
                .execute();

        // 기본 프로젝트 생성 (생성자: id=1)
        dsl.insertInto(PROJECT)
                .set(PROJECT.ID, TEST_PROJECT_ID)
                .set(PROJECT.TITLE, "테스트 프로젝트")
                .set(PROJECT.DESCRIPTION, "테스트 설명")
                .set(PROJECT.CONTENT, "테스트 내용")
                .set(PROJECT.CATEGORY, "CTF")
                .set(PROJECT.SUBCATEGORY, "포너블")
                .set(PROJECT.MAX_PARTICIPANTS_NUMBER, 10)
                .set(PROJECT.STARTED_AT, OffsetDateTime.now().minusDays(1))
                .set(PROJECT.ENDED_AT, OffsetDateTime.now().plusDays(30))
                .set(PROJECT.MEMBER_ID, 1L)
                .set(PROJECT.CREATED_AT, OffsetDateTime.now())
                .set(PROJECT.UPDATED_AT, OffsetDateTime.now())
                .execute();
    }

    private void setupSecurityContext() {
        CurrentUser currentUser = new CurrentUser(TEST_MEMBER_ID, "test", "test@example.com", "테스트 사용자", "MEMBER");
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

    private void setupChairmanSecurityContext() {
        CurrentUser chairmanUser = new CurrentUser(4L, "chairman", "chairman@example.com", "회장", "CHAIRMAN");
        UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(chairmanUser, null, chairmanUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void setupCreatorSecurityContext() {
        CurrentUser creatorUser = new CurrentUser(1L, "creator", "creator@example.com", "생성자", "MEMBER");
        UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(creatorUser, null, creatorUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void setupOtherUserSecurityContext() {
        CurrentUser otherUser = new CurrentUser(999L, "other", "other@example.com", "다른 사용자", "MEMBER");
        UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(otherUser, null, otherUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private ProjectJoinRequestDto createProjectJoinRequest() {
        ProjectJoinRequestDto request = new ProjectJoinRequestDto();
        request.setProjectId(TEST_PROJECT_ID);
        return request;
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
                .set(PROJECT.CREATED_AT, OffsetDateTime.now())
                .set(PROJECT.UPDATED_AT, OffsetDateTime.now())
                .execute();

        // 승인된 참가자 생성
        dsl.insertInto(PROJECT_PARTICIPANT)
                .set(PROJECT_PARTICIPANT.ID, projectId + 1000L)
                .set(PROJECT_PARTICIPANT.PROJECT_ID, projectId)
                .set(PROJECT_PARTICIPANT.MEMBER_ID, memberId)
                .set(PROJECT_PARTICIPANT.STATUS, ProjectParticipantStatus.APPROVED.name())
                .set(PROJECT_PARTICIPANT.CREATED_AT, OffsetDateTime.now())
                .set(PROJECT_PARTICIPANT.UPDATED_AT, OffsetDateTime.now())
                .execute();
    }

    private void createProjectParticipant(Long participantId, Long projectId, Long memberId, ProjectParticipantStatus status) {
        dsl.insertInto(PROJECT_PARTICIPANT)
                .set(PROJECT_PARTICIPANT.ID, participantId)
                .set(PROJECT_PARTICIPANT.PROJECT_ID, projectId)
                .set(PROJECT_PARTICIPANT.MEMBER_ID, memberId)
                .set(PROJECT_PARTICIPANT.STATUS, status.name())
                .set(PROJECT_PARTICIPANT.CREATED_AT, OffsetDateTime.now())
                .set(PROJECT_PARTICIPANT.UPDATED_AT, OffsetDateTime.now())
                .execute();
    }

    private void verifyNoParticipantCreated(Long projectId, Long memberId) {
        var participant = dsl.selectFrom(PROJECT_PARTICIPANT)
                .where(PROJECT_PARTICIPANT.PROJECT_ID.eq(projectId))
                .and(PROJECT_PARTICIPANT.MEMBER_ID.eq(memberId))
                .fetchOne();

        assertThat(participant).isNull();
    }

    private void verifyParticipantCreated(Long projectId, Long memberId, ProjectParticipantStatus expectedStatus) {
        var participant = dsl.selectFrom(PROJECT_PARTICIPANT)
                .where(PROJECT_PARTICIPANT.PROJECT_ID.eq(projectId))
                .and(PROJECT_PARTICIPANT.MEMBER_ID.eq(memberId))
                .and(PROJECT_PARTICIPANT.DELETED_AT.isNull())
                .fetchOne();

        assertThat(participant).isNotNull();
        assertThat(participant.getStatus()).isEqualTo(expectedStatus.name());
    }

    private void verifyParticipantStatusInDatabase(Long participantId, ProjectParticipantStatus expectedStatus) {
        var participant = dsl.selectFrom(PROJECT_PARTICIPANT)
                .where(PROJECT_PARTICIPANT.ID.eq(participantId))
                .and(PROJECT_PARTICIPANT.DELETED_AT.isNull())
                .fetchOne();

        assertThat(participant).isNotNull();
        assertThat(participant.getStatus()).isEqualTo(expectedStatus.name());
    }

    private void verifyParticipantSoftDeletedInDatabase(Long participantId) {
        var participant = dsl.selectFrom(PROJECT_PARTICIPANT)
                .where(PROJECT_PARTICIPANT.ID.eq(participantId))
                .fetchOne();

        assertThat(participant).isNotNull();
        assertThat(participant.getDeletedAt()).isNotNull();
    }

    private void verifyParticipantHardDeletedInDatabase(Long participantId) {
        var participant = dsl.selectFrom(PROJECT_PARTICIPANT)
                .where(PROJECT_PARTICIPANT.ID.eq(participantId))
                .fetchOne();

        assertThat(participant).isNull();
    }
}
