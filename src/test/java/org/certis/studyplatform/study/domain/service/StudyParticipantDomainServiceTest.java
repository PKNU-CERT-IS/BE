package org.certis.studyplatform.study.domain.service;

import org.certis.studyplatform.config.TestEmbeddedPostgresConfig;
import org.certis.studyplatform.member.domain.MemberRole;
import org.certis.studyplatform.member.domain.vo.MemberVo;
import org.certis.studyplatform.study.application.object.command.CreateStudyParticipantCommand;
import org.certis.studyplatform.study.application.object.command.UpdateStudyParticipantStatusCommand;
import org.certis.studyplatform.study.domain.StudyParticipantStatus;
import org.certis.studyplatform.study.domain.repository.StudyParticipantCommandRepository;
import org.certis.studyplatform.study.domain.repository.StudyParticipantQueryRepository;
import org.certis.studyplatform.study.domain.repository.StudyQueryRepository;
import org.certis.studyplatform.study.domain.vo.StudyParticipantCreatedVo;
import org.certis.studyplatform.study.domain.vo.StudyParticipantStatusUpdatedVo;
import org.certis.studyplatform.study.domain.vo.StudyParticipantVo;
import org.certis.studyplatform.study.domain.vo.StudyVo;
import org.certis.studyplatform.project.domain.repository.ProjectParticipantQueryRepository;
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
 * StudyParticipantDomainService 도메인 서비스 테스트
 * 
 * 🎯 테스트 목표:
 * - 새로운 비즈니스 규칙 검증 (참가 신청 제한)
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
@DisplayName("StudyParticipantDomainService 도메인 서비스 테스트")
class StudyParticipantDomainServiceTest {

    @Mock
    private StudyParticipantCommandRepository commandRepository;
    
    @Mock
    private StudyParticipantQueryRepository queryRepository;
    
    @Mock
    private StudyQueryRepository studyQueryRepository;
    
    @Mock
    private ProjectParticipantQueryRepository projectParticipantQueryRepository;
    
    @Mock
    private MemberQueryRepository memberQueryRepository;

    private StudyParticipantDomainService domainService;

    @BeforeEach
    void setUp() {
        domainService = new StudyParticipantDomainService(
                commandRepository,
                queryRepository,
                studyQueryRepository,
                projectParticipantQueryRepository,
                memberQueryRepository
        );
    }

    @Nested
    @DisplayName("참가 신청 제한 규칙 테스트")
    class ApplicationLimitsTest {

        @Test
        @DisplayName("진행 중인 스터디가 2개 이상이면 추가 신청 불가")
        void shouldRejectWhenActiveStudiesGreaterThanOrEqualTwo() {
            // Given
            Long memberId = 2L; // 스터디 생성자가 아닌 다른 사용자
            Long studyId = 1L;
            
            when(queryRepository.countActiveStudiesByMemberId(memberId)).thenReturn(2L);
            when(studyQueryRepository.findByIdAndDeletedAtIsNull(studyId))
                    .thenReturn(Optional.of(createStudyVo(studyId, 1L))); // 생성자는 1L
            when(queryRepository.existsByStudyIdAndMemberId(studyId, memberId)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> domainService.createParticipant(
                    createStudyParticipantCommand(studyId, memberId)))
                    .isInstanceOf(DomainException.class)
                    .hasMessage("프로젝트 미진행 시 스터디 2개까지 가능합니다.");
        }

        @Test
        @DisplayName("진행 중인 프로젝트 1개 + 스터디 1개면 추가 신청 불가")
        void shouldRejectWhenProjectAndStudyActive() {
            // Given
            Long memberId = 2L; // 스터디 생성자가 아닌 다른 사용자
            Long studyId = 1L;
            
            when(queryRepository.countActiveStudiesByMemberId(memberId)).thenReturn(1L);
            when(projectParticipantQueryRepository.countActiveProjectsByMemberId(memberId)).thenReturn(1L);
            when(studyQueryRepository.findByIdAndDeletedAtIsNull(studyId))
                    .thenReturn(Optional.of(createStudyVo(studyId, 1L))); // 생성자는 1L
            when(queryRepository.existsByStudyIdAndMemberId(studyId, memberId)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> domainService.createParticipant(
                    createStudyParticipantCommand(studyId, memberId)))
                    .isInstanceOf(DomainException.class)
                    .hasMessage("프로젝트 진행 중에는 스터디 1개까지만 신청할 수 있습니다.");
        }

        @Test
        @DisplayName("진행 중인 스터디 1개, 프로젝트 0개면 신청 가능")
        void shouldAllowWhenOnlyOneActiveStudy() {
            // Given
            Long memberId = 2L; // 스터디 생성자가 아닌 다른 사용자
            Long studyId = 1L;
            
            when(queryRepository.countActiveStudiesByMemberId(memberId)).thenReturn(1L);
            when(projectParticipantQueryRepository.countActiveProjectsByMemberId(memberId)).thenReturn(0L);
            when(studyQueryRepository.findByIdAndDeletedAtIsNull(studyId))
                    .thenReturn(Optional.of(createStudyVo(studyId, 1L))); // 생성자는 1L
            when(queryRepository.existsByStudyIdAndMemberId(studyId, memberId)).thenReturn(false);
            when(commandRepository.save(any())).thenReturn(createStudyParticipantCreatedVo());

            // When
            StudyParticipantCreatedVo result = domainService.createParticipant(
                    createStudyParticipantCommand(studyId, memberId));

            // Then
            assertThat(result).isNotNull();
            verify(commandRepository).save(any());
        }
    }

    @Nested
    @DisplayName("관리자 권한 테스트")
    class AdminPermissionTest {

        @Test
        @DisplayName("관리자(STAFF)는 스터디 생성자가 아니어도 승인 가능")
        void shouldAllowAdminToApprove() {
            // Given
            Long studyId = 1L;
            Long adminId = 2L;
            Long participantId = 1L;
            
            StudyVo study = createStudyVo(studyId, 1L); // 다른 사용자가 생성자
            
            when(studyQueryRepository.findByIdAndDeletedAtIsNull(studyId))
                    .thenReturn(Optional.of(study));
            when(studyQueryRepository.findById(studyId))
                    .thenReturn(Optional.of(study));
            when(memberQueryRepository.findRoleByMemberId(new org.certis.studyplatform.member.domain.vo.MemberIdVo(adminId)))
                    .thenReturn(Optional.of(org.certis.studyplatform.member.domain.MemberRole.STAFF));
            when(queryRepository.findById(participantId))
                    .thenReturn(Optional.of(createStudyParticipantVo(participantId, StudyParticipantStatus.PENDING)));
            when(commandRepository.updateStatus(any(), any())).thenReturn(createStudyParticipantStatusUpdatedVo());

            // When
            StudyParticipantStatusUpdatedVo result = domainService.approveParticipant(
                    createUpdateStatusCommand(participantId, adminId));

            // Then
            assertThat(result).isNotNull();
            verify(commandRepository).updateStatus(any(), any());
        }

        @Test
        @DisplayName("일반 사용자는 스터디 생성자가 아니면 승인 불가")
        void shouldRejectNonCreatorNonAdmin() {
            // Given
            Long studyId = 1L;
            Long userId = 2L;
            Long participantId = 1L;
            
            StudyVo study = createStudyVo(studyId, 1L); // 다른 사용자가 생성자
            
            when(studyQueryRepository.findByIdAndDeletedAtIsNull(studyId))
                    .thenReturn(Optional.of(study));
            when(memberQueryRepository.findRoleByMemberId(new org.certis.studyplatform.member.domain.vo.MemberIdVo(userId)))
                    .thenReturn(Optional.of(org.certis.studyplatform.member.domain.MemberRole.PLAYER));
            when(queryRepository.findById(participantId))
                    .thenReturn(Optional.of(createStudyParticipantVo(participantId, StudyParticipantStatus.PENDING)));

            // When & Then
            assertThatThrownBy(() -> domainService.approveParticipant(
                    createUpdateStatusCommand(participantId, userId)))
                    .isInstanceOf(DomainException.class)
                    .hasMessage("스터디 생성자 또는 관리자만 참가 승인/거절을 할 수 있습니다.");
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
            Long studyId = 1L;
            
            StudyParticipantVo participant = createStudyParticipantVo(participantId, StudyParticipantStatus.PENDING);
            when(queryRepository.findById(participantId)).thenReturn(Optional.of(participant));
            when(studyQueryRepository.findByIdAndDeletedAtIsNull(studyId))
                    .thenReturn(Optional.of(createStudyVo(studyId, requesterId)));
            when(memberQueryRepository.findRoleByMemberId(new org.certis.studyplatform.member.domain.vo.MemberIdVo(requesterId)))
                    .thenReturn(Optional.of(org.certis.studyplatform.member.domain.MemberRole.PLAYER));
            when(commandRepository.updateStatus(any(), any())).thenReturn(createStudyParticipantStatusUpdatedVo());

            // When
            StudyParticipantStatusUpdatedVo result = domainService.rejectParticipant(
                    createUpdateStatusCommand(participantId, requesterId));

            // Then
            assertThat(result).isNotNull();
            assertThat(result.currentStatus()).isEqualTo(StudyParticipantStatus.REJECTED);
            verify(commandRepository).updateStatus(any(), any());
        }

        @Test
        @DisplayName("승인된 참가자 취소 시 하드 삭제 수행")
        void shouldHardDeleteOnCancelApproved() {
            // Given
            Long participantId = 1L;
            Long requesterId = 1L;
            Long studyId = 1L;
            
            StudyParticipantVo participant = createStudyParticipantVo(participantId, StudyParticipantStatus.APPROVED);
            when(queryRepository.findById(participantId)).thenReturn(Optional.of(participant));
            when(studyQueryRepository.findByIdAndDeletedAtIsNull(studyId))
                    .thenReturn(Optional.of(createStudyVo(studyId, requesterId)));
            when(memberQueryRepository.findRoleByMemberId(new org.certis.studyplatform.member.domain.vo.MemberIdVo(requesterId)))
                    .thenReturn(Optional.of(org.certis.studyplatform.member.domain.MemberRole.PLAYER));

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
            
            StudyParticipantVo participant = createStudyParticipantVo(participantId, StudyParticipantStatus.PENDING);
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
        @DisplayName("존재하지 않는 스터디에 신청 시 예외 발생")
        void shouldThrowExceptionWhenStudyNotFound() {
            // Given
            Long studyId = 999L;
            Long memberId = 1L;
            
            when(studyQueryRepository.findByIdAndDeletedAtIsNull(studyId))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> domainService.createParticipant(
                    createStudyParticipantCommand(studyId, memberId)))
                    .isInstanceOf(DomainException.class)
                    .hasMessage("스터디를 찾을 수 없습니다.");
        }

        @Test
        @DisplayName("중복 신청 시 예외 발생")
        void shouldThrowExceptionWhenDuplicateApplication() {
            // Given
            Long studyId = 1L;
            Long memberId = 2L; // 스터디 생성자가 아닌 다른 사용자
            
            when(studyQueryRepository.findByIdAndDeletedAtIsNull(studyId))
                    .thenReturn(Optional.of(createStudyVo(studyId, 1L))); // 생성자는 1L
            when(queryRepository.existsByStudyIdAndMemberId(studyId, memberId)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> domainService.createParticipant(
                    createStudyParticipantCommand(studyId, memberId)))
                    .isInstanceOf(DomainException.class)
                    .hasMessage("이미 참가 신청한 스터디입니다.");
        }
    }

    // ================================================================
    // Helper Methods
    // ================================================================

    private CreateStudyParticipantCommand createStudyParticipantCommand(Long studyId, Long memberId) {
        return new CreateStudyParticipantCommand(
                studyId, memberId
        );
    }

    private UpdateStudyParticipantStatusCommand createUpdateStatusCommand(Long participantId, Long requesterId) {
        return new UpdateStudyParticipantStatusCommand(
                participantId, StudyParticipantStatus.APPROVED, requesterId
        );
    }

    private StudyVo createStudyVo(Long studyId) {
        return createStudyVo(studyId, 1L);
    }

    private StudyVo createStudyVo(Long studyId, Long creatorId) {
        OffsetDateTime endDate = OffsetDateTime.now().plusDays(30);
        return new StudyVo(
                studyId,
                "테스트 스터디",
                "테스트 설명",
                "테스트 내용",
                "CTF",
                "포너블",
                OffsetDateTime.now().minusDays(1),
                endDate,
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                creatorId,
                "생성자",
                org.certis.studyplatform.member.domain.MemberGrade.FRESHMAN,
                calculateSemester(endDate), // semester 계산
                calculateStatus(endDate), // status 계산
                10,
                0,
                true, // isParticipantable
                java.util.Collections.emptyList(), // attached
                java.util.Collections.emptyList(),
                java.util.Collections.emptyList()
        );
    }

    private String calculateSemester(OffsetDateTime endedAt) {
        if (endedAt == null) {
            return null;
        }
        
        java.time.LocalDate endDate = endedAt.toLocalDate();
        int year = endDate.getYear();
        int month = endDate.getMonthValue();
        
        if (month >= 3 && month <= 8) {
            return year + "-1"; // 1학기
        } else {
            return year + "-2"; // 2학기
        }
    }

    private String calculateStatus(OffsetDateTime endedAt) {
        if (endedAt == null) {
            return "ACTIVE"; // 종료일이 없으면 활성 상태
        }
        
        OffsetDateTime now = OffsetDateTime.now();
        return endedAt.isBefore(now) ? "ENDED" : "ACTIVE";
    }

    private StudyParticipantVo createStudyParticipantVo(Long participantId, StudyParticipantStatus status) {
        return new StudyParticipantVo(
                participantId,
                1L, // studyId
                1L, // memberId
                "테스트 사용자",
                status,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
    }

    private StudyParticipantCreatedVo createStudyParticipantCreatedVo() {
        return new StudyParticipantCreatedVo(
                1L, // id
                1L, // studyId
                1L, // memberId
                StudyParticipantStatus.PENDING,
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

    private StudyParticipantStatusUpdatedVo createStudyParticipantStatusUpdatedVo() {
        return new StudyParticipantStatusUpdatedVo(
                1L, // id
                1L, // studyId
                1L, // memberId
                StudyParticipantStatus.PENDING,
                StudyParticipantStatus.REJECTED, // REJECTED로 변경
                OffsetDateTime.now(),
                1L // requesterId
        );
    }
}
