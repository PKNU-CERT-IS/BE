package org.certis.studyplatform.project.domain.service;

import org.certis.studyplatform.member.domain.MemberRole;
import org.certis.studyplatform.member.domain.vo.MemberVo;
import org.certis.studyplatform.project.domain.ProjectParticipantStatus;
import org.certis.studyplatform.project.domain.repository.ProjectParticipantCommandRepository;
import org.certis.studyplatform.project.domain.repository.ProjectParticipantQueryRepository;
import org.certis.studyplatform.project.domain.repository.ProjectQueryRepository;
import org.certis.studyplatform.project.domain.vo.ProjectParticipantCreatedVo;
import org.certis.studyplatform.project.domain.vo.ProjectParticipantStatusUpdatedVo;
import org.certis.studyplatform.project.domain.vo.ProjectParticipantVo;
import org.certis.studyplatform.project.domain.vo.ProjectVo;
import org.certis.studyplatform.member.domain.repository.query.MemberQueryRepository;
import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ProjectParticipantDomainService 도메인 서비스 테스트
 * 
 * 🎯 테스트 목표:
 * - 프로젝트 참가 신청 제한 규칙 검증 (진행 중인 프로젝트 1개 제한)
 * - 관리자 권한 검증
 * - 거절/취소 동작 검증 (소프트/하드 삭제)
 * - 도메인 로직의 정확성 보장
 * 
 * 🔧 테스트 전략:
 * - Mock을 사용한 단위 테스트
 * - 각 비즈니스 규칙별 독립적인 테스트
 * - 예외 상황 및 엣지 케이스 커버
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectParticipantDomainService 도메인 서비스 테스트")
class ProjectParticipantDomainServiceTest {

    @Mock
    private ProjectParticipantCommandRepository commandRepository;
    
    @Mock
    private ProjectParticipantQueryRepository queryRepository;
    
    @Mock
    private ProjectQueryRepository projectQueryRepository;
    
    @Mock
    private MemberQueryRepository memberQueryRepository;

    private ProjectParticipantDomainService domainService;

    @BeforeEach
    void setUp() {
        domainService = new ProjectParticipantDomainService(
                commandRepository,
                queryRepository,
                projectQueryRepository,
                memberQueryRepository
        );
    }

    @Nested
    @DisplayName("참가 신청 제한 규칙 테스트")
    class ApplicationLimitsTest {

        @Test
        @DisplayName("진행 중인 프로젝트가 1개 이상이면 추가 신청 불가")
        void shouldRejectWhenActiveProjectsGreaterThanOrEqualOne() {
            // Given
            Long memberId = 2L;
            Long projectId = 1L;

            when(queryRepository.countActiveProjectsByMemberId(memberId)).thenReturn(1L);
            when(projectQueryRepository.findByIdAndDeletedAtIsNull(projectId))
                    .thenReturn(Optional.of(createProjectVo(projectId)));
            when(queryRepository.existsByProjectIdAndMemberId(projectId, memberId)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> domainService.createParticipant(
                    createProjectParticipantCommand(projectId, memberId)))
                    .isInstanceOf(DomainException.class)
                    .hasMessage("진행 중인 프로젝트가 1개 있으면 추가 신청이 불가합니다.");
        }

        @Test
        @DisplayName("진행 중인 프로젝트가 0개면 신청 가능")
        void shouldAllowWhenNoActiveProjects() {
            // Given
            Long memberId = 2L;
            Long projectId = 1L;

            when(queryRepository.countActiveProjectsByMemberId(memberId)).thenReturn(0L);
            when(projectQueryRepository.findByIdAndDeletedAtIsNull(projectId))
                    .thenReturn(Optional.of(createProjectVo(projectId)));
            when(queryRepository.existsByProjectIdAndMemberId(projectId, memberId)).thenReturn(false);
            when(commandRepository.save(any())).thenReturn(createProjectParticipantCreatedVo());

            // When
            ProjectParticipantCreatedVo result = domainService.createParticipant(
                    createProjectParticipantCommand(projectId, memberId));

            // Then
            assertThat(result).isNotNull();
            verify(commandRepository).save(any());
        }

        @Test
        @DisplayName("프로젝트 생성자의 자가 신청은 불가")
        void shouldRejectCreatorSelfApplicationInLimitsTest() {
            // Given
            Long memberId = 1L;
            Long projectId = 1L;

            when(projectQueryRepository.findByIdAndDeletedAtIsNull(projectId))
                    .thenReturn(Optional.of(createProjectVo(projectId, memberId))); // 같은 사용자가 생성자

            // When & Then
            assertThatThrownBy(() -> domainService.createParticipant(
                    createProjectParticipantCommand(projectId, memberId)))
                    .isInstanceOf(DomainException.class)
                    .hasMessage("프로젝트 생성자는 자신의 프로젝트에 참가 신청할 수 없습니다.");
            verify(commandRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("관리자 권한 테스트")
    class AdminPermissionTest {

        @Test
        @DisplayName("관리자(STAFF)는 프로젝트 생성자가 아니어도 승인 가능")
        void shouldAllowAdminToApprove() {
            // Given
            Long projectId = 1L;
            Long adminId = 2L;
            Long participantId = 1L;
            
            ProjectVo project = createProjectVo(projectId, 1L); // 다른 사용자가 생성자
            MemberVo admin = createMemberVo(adminId, MemberRole.STAFF);
            
            when(projectQueryRepository.findByIdAndDeletedAtIsNull(projectId))
                    .thenReturn(Optional.of(project));
            when(queryRepository.findById(participantId))
                    .thenReturn(Optional.of(createProjectParticipantVo(participantId, ProjectParticipantStatus.PENDING)));
            when(memberQueryRepository.findRoleByMemberId(new org.certis.studyplatform.member.domain.vo.MemberIdVo(adminId)))
                    .thenReturn(Optional.of(MemberRole.STAFF));
            when(projectQueryRepository.findById(projectId)).thenReturn(Optional.of(project));
            when(commandRepository.updateStatus(any())).thenReturn(
                    ProjectParticipantStatusUpdatedVo.of(
                            participantId,
                            projectId,
                            1L,
                            ProjectParticipantStatus.PENDING,
                            ProjectParticipantStatus.APPROVED
                    )
            );

            // When
            ProjectParticipantStatusUpdatedVo result = domainService.approveParticipant(
                    createUpdateStatusCommand(participantId, adminId));

            // Then
            assertThat(result).isNotNull();
            verify(commandRepository).updateStatus(any());
        }

        @Test
        @DisplayName("일반 사용자는 프로젝트 생성자가 아니면 승인 불가")
        void shouldRejectNonCreatorNonAdmin() {
            // Given
            Long projectId = 1L;
            Long userId = 2L;
            Long participantId = 1L;
            
            ProjectVo project = createProjectVo(projectId, 1L); // 다른 사용자가 생성자
            
            when(projectQueryRepository.findByIdAndDeletedAtIsNull(projectId))
                    .thenReturn(Optional.of(project));
            when(queryRepository.findById(participantId))
                    .thenReturn(Optional.of(createProjectParticipantVo(participantId, ProjectParticipantStatus.PENDING)));
            when(memberQueryRepository.findRoleByMemberId(new org.certis.studyplatform.member.domain.vo.MemberIdVo(userId)))
                    .thenReturn(Optional.of(MemberRole.PLAYER));

            // When & Then
            assertThatThrownBy(() -> domainService.approveParticipant(
                    createUpdateStatusCommand(participantId, userId)))
                    .isInstanceOf(DomainException.class)
                    .hasMessage("프로젝트 생성자 또는 관리자만 참가 승인/거절을 할 수 있습니다.");
        }

        @Test
        @DisplayName("CHAIRMAN 권한도 승인 가능")
        void shouldAllowChairmanToApprove() {
            // Given
            Long projectId = 1L;
            Long chairmanId = 2L;
            Long participantId = 1L;
            
            ProjectVo project = createProjectVo(projectId, 1L); // 다른 사용자가 생성자
            MemberVo chairman = createMemberVo(chairmanId, MemberRole.CHAIRMAN);
            
            when(projectQueryRepository.findByIdAndDeletedAtIsNull(projectId))
                    .thenReturn(Optional.of(project));
            when(queryRepository.findById(participantId))
                    .thenReturn(Optional.of(createProjectParticipantVo(participantId, ProjectParticipantStatus.PENDING)));
            when(memberQueryRepository.findRoleByMemberId(new org.certis.studyplatform.member.domain.vo.MemberIdVo(chairmanId)))
                    .thenReturn(Optional.of(MemberRole.CHAIRMAN));
            when(projectQueryRepository.findById(projectId)).thenReturn(Optional.of(project));
            when(commandRepository.updateStatus(any())).thenReturn(
                    ProjectParticipantStatusUpdatedVo.of(
                            participantId,
                            projectId,
                            1L,
                            ProjectParticipantStatus.PENDING,
                            ProjectParticipantStatus.APPROVED
                    )
            );

            // When
            ProjectParticipantStatusUpdatedVo result = domainService.approveParticipant(
                    createUpdateStatusCommand(participantId, chairmanId));

            // Then
            assertThat(result).isNotNull();
            verify(commandRepository).updateStatus(any());
        }
    }

    @Nested
    @DisplayName("거절/취소 동작 테스트")
    class RejectCancelBehaviorTest {

        @Test
        @DisplayName("거절 시 소프트 삭제 수행")
        void shouldSoftDeleteOnReject() {
            // Given
            Long participantId = 1L;
            Long requesterId = 1L;
            Long projectId = 1L;
            
            ProjectParticipantVo participant = createProjectParticipantVo(participantId, ProjectParticipantStatus.PENDING);
            when(queryRepository.findById(participantId)).thenReturn(Optional.of(participant));
            when(projectQueryRepository.findByIdAndDeletedAtIsNull(projectId))
                    .thenReturn(Optional.of(createProjectVo(projectId, requesterId)));

            // When
            ProjectParticipantStatusUpdatedVo result = domainService.rejectParticipant(
                    createUpdateStatusCommand(participantId, requesterId));

            // Then
            assertThat(result).isNotNull();
            assertThat(result.currentStatus()).isEqualTo(ProjectParticipantStatus.REJECTED);
            verify(commandRepository).softDeleteById(participantId);
            verify(commandRepository, never()).updateStatus(any());
        }

        @Test
        @DisplayName("승인된 참가자 취소 시 하드 삭제 수행")
        void shouldHardDeleteOnCancelApproved() {
            // Given
            Long participantId = 1L;
            Long requesterId = 1L;
            Long projectId = 1L;
            
            ProjectParticipantVo participant = createProjectParticipantVo(participantId, ProjectParticipantStatus.APPROVED);
            when(queryRepository.findById(participantId)).thenReturn(Optional.of(participant));
            when(projectQueryRepository.findByIdAndDeletedAtIsNull(projectId))
                    .thenReturn(Optional.of(createProjectVo(projectId, requesterId)));

            // When
            domainService.cancelApprovedParticipant(participantId, requesterId);

            // Then
            verify(commandRepository).deleteByIdHard(participantId);
        }

        @Test
        @DisplayName("승인되지 않은 참가자 취소 시 예외 발생")
        void shouldThrowExceptionWhenCancellingNonApprovedParticipant() {
            // Given
            Long participantId = 1L;
            Long requesterId = 1L;
            
            ProjectParticipantVo participant = createProjectParticipantVo(participantId, ProjectParticipantStatus.PENDING);
            when(queryRepository.findById(participantId)).thenReturn(Optional.of(participant));

            // When & Then
            assertThatThrownBy(() -> domainService.cancelApprovedParticipant(participantId, requesterId))
                    .isInstanceOf(DomainException.class)
                    .hasMessage("승인된 참가자만 취소할 수 있습니다.");
        }

        @Test
        @DisplayName("거절된 참가자 취소 시 예외 발생")
        void shouldThrowExceptionWhenCancellingRejectedParticipant() {
            // Given
            Long participantId = 1L;
            Long requesterId = 1L;
            
            ProjectParticipantVo participant = createProjectParticipantVo(participantId, ProjectParticipantStatus.REJECTED);
            when(queryRepository.findById(participantId)).thenReturn(Optional.of(participant));

            // When & Then
            assertThatThrownBy(() -> domainService.cancelApprovedParticipant(participantId, requesterId))
                    .isInstanceOf(DomainException.class)
                    .hasMessage("승인된 참가자만 취소할 수 있습니다.");
        }
    }

    @Nested
    @DisplayName("예외 상황 테스트")
    class ExceptionScenariosTest {

        @Test
        @DisplayName("존재하지 않는 프로젝트에 신청 시 예외 발생")
        void shouldThrowExceptionWhenProjectNotFound() {
            // Given
            Long projectId = 999L;
            Long memberId = 2L;

            when(projectQueryRepository.findByIdAndDeletedAtIsNull(projectId))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> domainService.createParticipant(
                    createProjectParticipantCommand(projectId, memberId)))
                    .isInstanceOf(DomainException.class)
                    .hasMessage("프로젝트를 찾을 수 없습니다.");
        }

        @Test
        @DisplayName("중복 신청 시 예외 발생")
        void shouldThrowExceptionWhenDuplicateApplication() {
            // Given
            Long projectId = 1L;
            Long memberId = 2L;

            when(projectQueryRepository.findByIdAndDeletedAtIsNull(projectId))
                    .thenReturn(Optional.of(createProjectVo(projectId)));
            when(queryRepository.existsByProjectIdAndMemberId(projectId, memberId)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> domainService.createParticipant(
                    createProjectParticipantCommand(projectId, memberId)))
                    .isInstanceOf(DomainException.class)
                    .hasMessage("이미 참가 신청한 프로젝트입니다.");
        }

        @Test
        @DisplayName("프로젝트 생성자의 자가 신청 시 예외 발생")
        void shouldThrowExceptionWhenCreatorSelfApplication() {
            // Given
            Long projectId = 1L;
            Long memberId = 1L;

            when(projectQueryRepository.findByIdAndDeletedAtIsNull(projectId))
                    .thenReturn(Optional.of(createProjectVo(projectId, memberId))); // 같은 사용자가 생성자

            // When & Then
            assertThatThrownBy(() -> domainService.createParticipant(
                    createProjectParticipantCommand(projectId, memberId)))
                    .isInstanceOf(DomainException.class)
                    .hasMessage("프로젝트 생성자는 자신의 프로젝트에 참가 신청할 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("참가자 수 제한 테스트")
    class ParticipantLimitTest {

        @Test
        @DisplayName("정원 초과 신청 시 예외 발생")
        void shouldThrowExceptionWhenExceedingMaxParticipants() {
            // Given
            Long projectId = 1L;
            Long memberId = 2L;

            ProjectVo project = new ProjectVo(
                    projectId,
                    "테스트 프로젝트",
                    "테스트 설명",
                    "테스트 내용",
                    "CTF",
                    "포너블",
                    OffsetDateTime.now().minusDays(1),
                    OffsetDateTime.now().plusDays(30),
                    1L, // creatorId
                    "생성자",
                    "4", // creatorGrade
                    "2024-1", // semester
                    "ACTIVE", // status
                    null, // githubUrl
                    null, // externalUrl
                    null, // demoUrl
                    null, // thumbnailUrl
                    2, // maxParticipants
                    0, // currentParticipants
                    true, // isParticipantable
                    java.util.Collections.emptyList(), // attached
                    java.util.Collections.emptyList() // meetingSummaryVos
            );

            when(projectQueryRepository.findByIdAndDeletedAtIsNull(projectId))
                    .thenReturn(Optional.of(project));
            when(queryRepository.existsByProjectIdAndMemberId(projectId, memberId)).thenReturn(false);
            when(queryRepository.countApprovedParticipantsByProjectId(projectId)).thenReturn(2L);

            // When & Then
            assertThatThrownBy(() -> domainService.createParticipant(
                    createProjectParticipantCommand(projectId, memberId)))
                    .isInstanceOf(DomainException.class)
                    .hasMessage("프로젝트 정원이 가득 찼습니다.");
        }
    }

    // ================================================================
    // Helper Methods
    // ================================================================

    private org.certis.studyplatform.project.application.object.command.CreateProjectParticipantCommand createProjectParticipantCommand(Long projectId, Long memberId) {
        return new org.certis.studyplatform.project.application.object.command.CreateProjectParticipantCommand(
                projectId, memberId
        );
    }

    private org.certis.studyplatform.project.application.object.command.UpdateProjectParticipantStatusCommand createUpdateStatusCommand(Long participantId, Long requesterId) {
        return new org.certis.studyplatform.project.application.object.command.UpdateProjectParticipantStatusCommand(
                participantId, ProjectParticipantStatus.APPROVED, requesterId
        );
    }

    private ProjectVo createProjectVo(Long projectId) {
        return createProjectVo(projectId, 1L);
    }

    private ProjectVo createProjectVo(Long projectId, Long creatorId) {
        return new ProjectVo(
                projectId,
                "테스트 프로젝트",
                "테스트 설명",
                "테스트 내용",
                "CTF",
                "포너블",
                OffsetDateTime.now().minusDays(1),
                OffsetDateTime.now().plusDays(30),
                creatorId,
                "생성자",
                "4", // creatorGrade
                "2024-1", // semester
                "ACTIVE", // status
                null, // githubUrl
                null, // externalUrl
                null, // demoUrl
                null, // thumbnailUrl
                10, // maxParticipants
                0, // currentParticipants
                true, // isParticipantable
                java.util.Collections.emptyList(), // attached
                java.util.Collections.emptyList() // meetingSummaryVos
        );
    }

    private ProjectParticipantVo createProjectParticipantVo(Long participantId, ProjectParticipantStatus status) {
        return new ProjectParticipantVo(
                participantId,
                1L, // projectId
                1L, // memberId
                "테스트 사용자",
                status,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
    }

    private ProjectParticipantCreatedVo createProjectParticipantCreatedVo() {
        return new ProjectParticipantCreatedVo(
                1L, // id
                1L, // projectId
                1L, // memberId
                ProjectParticipantStatus.PENDING,
                OffsetDateTime.now()
        );
    }

    private MemberVo createMemberVo(Long memberId, MemberRole role) {
        return new MemberVo(
                memberId,
                "테스트 사용자",
                "20240001",
                null,
                org.certis.studyplatform.member.domain.MemberGrade.FRESHMAN,
                role,
                java.util.Collections.emptyList(),
                "컴퓨터공학과",
                "테스트 설명",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
    }
}
