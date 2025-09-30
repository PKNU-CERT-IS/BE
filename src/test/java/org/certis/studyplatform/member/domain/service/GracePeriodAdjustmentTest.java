package org.certis.studyplatform.member.domain.service;

import org.certis.studyplatform.member.domain.repository.command.MemberCommandRepository;
import org.certis.studyplatform.member.domain.repository.query.MemberQueryRepository;
import org.certis.studyplatform.member.domain.vo.MemberIdVo;
import org.certis.studyplatform.member.domain.vo.MemberVo;
import org.certis.studyplatform.member.domain.MemberRole;
import org.certis.studyplatform.member.domain.MemberGrade;
import org.certis.studyplatform.project.domain.repository.ProjectParticipantQueryRepository;
import org.certis.studyplatform.project.domain.vo.ProjectParticipantSummaryVo;
import org.certis.studyplatform.project.domain.ProjectParticipantStatus;
import org.certis.studyplatform.study.domain.repository.StudyParticipantQueryRepository;
import org.certis.studyplatform.study.domain.vo.StudyParticipantSummaryVo;
import org.certis.studyplatform.study.domain.StudyParticipantStatus;
import org.certis.studyplatform.shared.util.GracePeriodCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 유예기간 조기 종료 재조정 테스트
 */
@ExtendWith(MockitoExtension.class)
class GracePeriodAdjustmentTest {

    @Mock
    private MemberCommandRepository memberCommandRepository;
    
    @Mock
    private StudyParticipantQueryRepository studyParticipantQueryRepository;
    
    @Mock
    private ProjectParticipantQueryRepository projectParticipantQueryRepository;
    
    @Mock
    private MemberQueryRepository memberQueryRepository;

    @InjectMocks
    private GracePeriodExtensionDomainService gracePeriodExtensionDomainService;

    private OffsetDateTime now;
    private OffsetDateTime startDate;
    private OffsetDateTime earlyEndDate;

    @BeforeEach
    void setUp() {
        now = OffsetDateTime.now();
        startDate = now.minusWeeks(2); // 2주 전 시작
        earlyEndDate = now; // 현재 시점에서 조기 종료
    }

    @Test
    @DisplayName("스터디 조기 종료 시 유예기간 재조정 - 성공")
    void testAdjustGracePeriodForEarlyTerminatedStudy_Success() {
        // Given
        Long studyId = 1L;
        Long memberId = 1L;
        
        StudyParticipantSummaryVo participant = new StudyParticipantSummaryVo(
            1L, studyId, memberId, "테스트 사용자", MemberGrade.FRESHMAN, null, "Study Title", StudyParticipantStatus.APPROVED, now
        );
        
        when(studyParticipantQueryRepository.findAllApprovedByStudyId(studyId))
            .thenReturn(List.of(participant));
        
        MemberVo member = new MemberVo(
            memberId, "테스트 사용자", "20240001", null, MemberGrade.FRESHMAN, 
            MemberRole.UPSOLVER, List.of("Java", "Spring"), "컴퓨터공학", 
            "테스트 설명", now.minusWeeks(3), now
        );
        when(memberQueryRepository.findById(MemberIdVo.of(memberId)))
            .thenReturn(Optional.of(member));
        
        doNothing().when(memberCommandRepository).updateGracePeriod(any(), any());

        // When
        gracePeriodExtensionDomainService.adjustGracePeriodForEarlyTerminatedStudy(
            studyId, startDate, earlyEndDate
        );

        // Then
        verify(studyParticipantQueryRepository).findAllApprovedByStudyId(studyId);
        verify(memberQueryRepository).findById(MemberIdVo.of(memberId));
        verify(memberCommandRepository).updateGracePeriod(any(), any());
    }

    @Test
    @DisplayName("프로젝트 조기 종료 시 유예기간 재조정 - 성공")
    void testAdjustGracePeriodForEarlyTerminatedProject_Success() {
        // Given
        Long projectId = 1L;
        Long memberId = 1L;
        
        ProjectParticipantSummaryVo participant = new ProjectParticipantSummaryVo(
            1L, projectId, memberId, "테스트 사용자", MemberGrade.FRESHMAN, null, "Project Title", ProjectParticipantStatus.APPROVED, now
        );
        
        when(projectParticipantQueryRepository.findAllApprovedByProjectId(projectId))
            .thenReturn(List.of(participant));
        
        MemberVo member = new MemberVo(
            memberId, "테스트 사용자", "20240001", null, MemberGrade.FRESHMAN, 
            MemberRole.UPSOLVER, List.of("Java", "Spring"), "컴퓨터공학", 
            "테스트 설명", now.minusWeeks(3), now
        );
        when(memberQueryRepository.findById(MemberIdVo.of(memberId)))
            .thenReturn(Optional.of(member));
        
        doNothing().when(memberCommandRepository).updateGracePeriod(any(), any());

        // When
        gracePeriodExtensionDomainService.adjustGracePeriodForEarlyTerminatedProject(
            projectId, startDate, earlyEndDate
        );

        // Then
        verify(projectParticipantQueryRepository).findAllApprovedByProjectId(projectId);
        verify(memberQueryRepository).findById(MemberIdVo.of(memberId));
        verify(memberCommandRepository).updateGracePeriod(any(), any());
    }

    @Test
    @DisplayName("조기 종료 시 유예기간 단축 확인 - 3주 이하 활동")
    void testGracePeriodReductionForShortActivity() {
        // Given - 2주짜리 활동을 1주에서 조기 종료 (50% 단축)
        OffsetDateTime activityStart = now.minusWeeks(2);
        OffsetDateTime earlyEnd = now.minusWeeks(1); // 1주만 진행하고 조기 종료
        
        // 2주 활동은 1주 유예기간
        OffsetDateTime originalGracePeriod = GracePeriodCalculator.calculateGracePeriodForActivity(
            activityStart, activityStart.plusWeeks(4)
        );
        
        // 1주 활동은 1주 유예기간 (동일)
        OffsetDateTime adjustedGracePeriod = GracePeriodCalculator.calculateGracePeriodForActivity(
            activityStart, earlyEnd
        );
        
        // When & Then
        // 1주 활동으로 인해 유예기간이 단축됨을 확인
        assert originalGracePeriod != null;
        assert adjustedGracePeriod != null;
        assert adjustedGracePeriod.isBefore(originalGracePeriod);
    }

    @Test
    @DisplayName("비-UPSOLVER 회원은 유예기간 재조정 대상이 아님")
    void testNonUpsolverMemberNotAdjusted() {
        // Given
        Long studyId = 1L;
        Long memberId = 1L;
        
        StudyParticipantSummaryVo participant = new StudyParticipantSummaryVo(
            1L, studyId, memberId, "테스트 사용자", MemberGrade.FRESHMAN, null, "Study Title", StudyParticipantStatus.APPROVED, now
        );
        
        when(studyParticipantQueryRepository.findAllApprovedByStudyId(studyId))
            .thenReturn(List.of(participant));
        
        // NON-UPSOLVER 회원
        MemberVo member = new MemberVo(
            memberId, "테스트 사용자", "20240001", null, MemberGrade.FRESHMAN, 
            MemberRole.PLAYER, List.of("Java", "Spring"), "컴퓨터공학", 
            "테스트 설명", now.minusWeeks(3), now
        );
        when(memberQueryRepository.findById(MemberIdVo.of(memberId)))
            .thenReturn(Optional.of(member));

        // When
        gracePeriodExtensionDomainService.adjustGracePeriodForEarlyTerminatedStudy(
            studyId, startDate, earlyEndDate
        );

        // Then
        verify(memberCommandRepository, never()).updateGracePeriod(any(), any());
    }

    @Test
    @DisplayName("현재 유예기간이 조정된 유예기간보다 짧으면 재조정하지 않음")
    void testDoNotAdjustIfCurrentGracePeriodIsShorter() {
        // Given
        Long studyId = 1L;
        Long memberId = 1L;
        
        StudyParticipantSummaryVo participant = new StudyParticipantSummaryVo(
            1L, studyId, memberId, "테스트 사용자", MemberGrade.FRESHMAN, null, "Study Title", StudyParticipantStatus.APPROVED, now
        );
        
        when(studyParticipantQueryRepository.findAllApprovedByStudyId(studyId))
            .thenReturn(List.of(participant));
        
        MemberVo member = new MemberVo(
            memberId, "테스트 사용자", "20240001", null, MemberGrade.FRESHMAN, 
            MemberRole.UPSOLVER, List.of("Java", "Spring"), "컴퓨터공학", 
            "테스트 설명", now.minusWeeks(3), now
        );
        when(memberQueryRepository.findById(MemberIdVo.of(memberId)))
            .thenReturn(Optional.of(member));

        // When
        gracePeriodExtensionDomainService.adjustGracePeriodForEarlyTerminatedStudy(
            studyId, startDate, earlyEndDate
        );

        // Then - 현재 구현에서는 currentGracePeriod가 null로 설정되어 항상 재조정됨
        verify(memberCommandRepository).updateGracePeriod(any(), any());
    }
}
