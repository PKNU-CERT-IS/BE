package org.certis.studyplatform.integration;

import org.certis.studyplatform.study.application.object.command.CreateStudyCommand;
import org.certis.studyplatform.study.application.object.command.CreateStudyParticipantCommand;
import org.certis.studyplatform.study.domain.repository.StudyCommandRepository;
import org.certis.studyplatform.study.domain.repository.StudyParticipantCommandRepository;
import org.certis.studyplatform.study.domain.repository.StudyParticipantQueryRepository;
import org.certis.studyplatform.study.domain.repository.StudyQueryRepository;
import org.certis.studyplatform.study.domain.service.StudyDomainService;
import org.certis.studyplatform.study.domain.service.StudyParticipantDomainService;
import org.certis.studyplatform.study.domain.vo.StudyVo;
import org.certis.studyplatform.project.application.object.command.CreateProjectCommand;
import org.certis.studyplatform.project.application.object.command.CreateProjectParticipantCommand;
import org.certis.studyplatform.project.domain.repository.ProjectCommandRepository;
import org.certis.studyplatform.project.domain.repository.ProjectParticipantCommandRepository;
import org.certis.studyplatform.project.domain.repository.ProjectParticipantQueryRepository;
import org.certis.studyplatform.project.domain.repository.ProjectQueryRepository;
import org.certis.studyplatform.project.domain.service.ProjectDomainService;
import org.certis.studyplatform.project.domain.service.ProjectParticipantDomainService;
import org.certis.studyplatform.project.domain.vo.ProjectVo;
import org.certis.studyplatform.member.domain.service.MemberDomainService;
import org.certis.studyplatform.member.domain.repository.query.MemberQueryRepository;
import org.certis.studyplatform.shared.service.S3FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.OffsetDateTime;
import java.time.DayOfWeek;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 생성 제한 vs 참가 신청 제한 통합 테스트
 * 
 * 🎯 테스트 목표:
 * - 생성 제한과 참가 신청 제한이 명확히 구분되는지 검증
 * - 동일한 사용자가 생성은 자유롭지만 참가 신청은 제한되는지 확인
 * - 비즈니스 규칙의 일관성 보장
 * 
 * 🔧 테스트 전략:
 * - Mock을 사용한 통합 테스트
 * - 생성과 참가 신청을 동시에 테스트
 * - 제한 규칙의 차이점을 명확히 검증
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("생성 제한 vs 참가 신청 제한 통합 테스트")
class CreationVsParticipationLimitsTest {

    // Study Domain Services
    @Mock
    private StudyCommandRepository studyCommandRepository;
    @Mock
    private StudyQueryRepository studyQueryRepository;
    @Mock
    private StudyParticipantCommandRepository studyParticipantCommandRepository;
    @Mock
    private StudyParticipantQueryRepository studyParticipantQueryRepository;
    
    // Project Domain Services
    @Mock
    private ProjectCommandRepository projectCommandRepository;
    @Mock
    private ProjectQueryRepository projectQueryRepository;
    @Mock
    private ProjectParticipantCommandRepository projectParticipantCommandRepository;
    @Mock
    private ProjectParticipantQueryRepository projectParticipantQueryRepository;
    
    // Common Services
    @Mock
    private MemberDomainService memberDomainService;
    @Mock
    private MemberQueryRepository memberQueryRepository;
    @Mock
    private S3FileService s3FileService;

    private StudyDomainService studyDomainService;
    private StudyParticipantDomainService studyParticipantDomainService;
    private ProjectDomainService projectDomainService;
    private ProjectParticipantDomainService projectParticipantDomainService;

    @BeforeEach
    void setUp() {
        studyDomainService = new StudyDomainService(
                studyCommandRepository,
                studyQueryRepository,
                memberDomainService,
                memberQueryRepository,
                studyParticipantQueryRepository,
                projectParticipantQueryRepository,
                projectQueryRepository
        );
        
        studyParticipantDomainService = new StudyParticipantDomainService(
                studyParticipantCommandRepository,
                studyParticipantQueryRepository,
                studyQueryRepository,
                studyCommandRepository,
                projectParticipantQueryRepository,
                projectQueryRepository,
                memberQueryRepository
        );
        
        projectDomainService = new ProjectDomainService(
                projectCommandRepository,
                projectQueryRepository,
                memberDomainService,
                memberQueryRepository,
                projectParticipantQueryRepository
        );
        
        projectParticipantDomainService = new ProjectParticipantDomainService(
                projectParticipantCommandRepository,
                projectParticipantQueryRepository,
                projectQueryRepository,
                projectCommandRepository,
                memberQueryRepository
        );
        when(memberQueryRepository.findByIdForUpdate(any()))
                .thenReturn(Optional.of(mock(org.certis.studyplatform.member.domain.vo.MemberVo.class)));
    }

    @Nested
    @DisplayName("스터디 생성 vs 참가 신청 제한 비교")
    class StudyCreationVsParticipationTest {

        @Test
        @DisplayName("진행 중인 스터디 2개 + 프로젝트 1개 상황에서 생성도 불가, 신청도 불가")
        void shouldAllowCreationButRejectParticipation() {
            // Given
            Long memberId = 1L;
            
            // 진행 중인 스터디 2개, 프로젝트 1개 상황 시뮬레이션
            when(studyParticipantQueryRepository.countActiveStudiesByMemberId(memberId)).thenReturn(2L);
            when(projectParticipantQueryRepository.countActiveProjectsByMemberId(memberId)).thenReturn(1L);
            
            // 스터디 생성은 제한 없음
            when(studyCommandRepository.save(any(StudyVo.class)))
                    .thenReturn(createStudyVo(1L, memberId));
            
            // 스터디 참가 신청은 제한 있음
            when(studyQueryRepository.findByIdAndDeletedAtIsNull(anyLong()))
                    .thenReturn(Optional.of(createStudyVo(2L, 2L))); // 다른 사용자가 생성한 스터디
            when(studyParticipantQueryRepository.findByStudyIdAndMemberId(anyLong(), eq(memberId)))
                    .thenReturn(Optional.empty());

            // When & Then - 스터디 생성은 제한되어야 함 (생성 자체 제한 정책 반영)
            CreateStudyCommand createCommand = createStudyCommand(memberId);
            assertThatThrownBy(() -> studyDomainService.createStudy(createCommand))
                    .isInstanceOf(org.certis.studyplatform.exception.DomainException.class)
                    .hasMessageContaining("프로젝트 진행 중에는 스터디 1개까지만");

            // When & Then - 스터디 참가 신청은 실패해야 함
            CreateStudyParticipantCommand participateCommand = new CreateStudyParticipantCommand(2L, memberId);
            
            assertThatThrownBy(() -> studyParticipantDomainService.createParticipant(participateCommand))
                    .isInstanceOf(org.certis.studyplatform.exception.DomainException.class)
                    .hasMessage("프로젝트 진행 중에는 스터디 1개까지만 신청할 수 있습니다.");
        }

        @Test
        @DisplayName("진행 중인 스터디 2개 상황에서 생성도 불가, 신청도 불가")
        void shouldAllowCreationButRejectParticipationWithTwoStudies() {
            // Given
            Long memberId = 1L;
            
            // 진행 중인 스터디 2개, 프로젝트 0개 상황 시뮬레이션
            when(studyParticipantQueryRepository.countActiveStudiesByMemberId(memberId)).thenReturn(2L);
            when(projectParticipantQueryRepository.countActiveProjectsByMemberId(memberId)).thenReturn(0L);
            
            // 스터디 생성은 제한 없음
            when(studyCommandRepository.save(any(StudyVo.class)))
                    .thenReturn(createStudyVo(1L, memberId));
            
            // 스터디 참가 신청은 제한 있음
            when(studyQueryRepository.findByIdAndDeletedAtIsNull(anyLong()))
                    .thenReturn(Optional.of(createStudyVo(2L, 2L))); // 다른 사용자가 생성한 스터디
            when(studyParticipantQueryRepository.findByStudyIdAndMemberId(anyLong(), eq(memberId)))
                    .thenReturn(Optional.empty());

            // When & Then - 스터디 생성은 제한되어야 함 (생성 자체 제한 정책 반영)
            CreateStudyCommand createCommand = createStudyCommand(memberId);
            assertThatThrownBy(() -> studyDomainService.createStudy(createCommand))
                    .isInstanceOf(org.certis.studyplatform.exception.DomainException.class)
                    .hasMessageContaining("스터디 2개까지");

            // When & Then - 스터디 참가 신청은 실패해야 함
            CreateStudyParticipantCommand participateCommand = new CreateStudyParticipantCommand(2L, memberId);
            
            assertThatThrownBy(() -> studyParticipantDomainService.createParticipant(participateCommand))
                    .isInstanceOf(org.certis.studyplatform.exception.DomainException.class)
                    .hasMessage("프로젝트 미진행 시 스터디 2개까지 가능합니다.");
        }

        @Test
        @DisplayName("생성한 스터디만 2개 있어도 추가 신청 불가(생성+참여 합산)")
        void shouldRejectWhenTwoActiveCreatedStudiesEvenIfNoJoined() {
            // Given
            Long memberId = 1L;

            // 참여 중 카운트는 0
            when(studyParticipantQueryRepository.countActiveStudiesByMemberId(memberId)).thenReturn(0L);
            when(projectParticipantQueryRepository.countActiveProjectsByMemberId(memberId)).thenReturn(0L);

            // 생성(leader) 기준 진행 중 스터디 2건으로 집계되도록 카운트 API 스텁 (도메인 로직은 집계 카운트를 사용)
            when(studyQueryRepository.countActiveStudiesCreatedByMemberId(memberId)).thenReturn(2L);
            when(projectQueryRepository.countActiveProjectsCreatedByMemberId(memberId)).thenReturn(0L);

            // 활성 스터디 목록(요약) 2건 반환
            var monday = OffsetDateTime.now().plusWeeks(1).with(DayOfWeek.MONDAY);
            var summaries = java.util.List.of(
                    new org.certis.studyplatform.study.domain.vo.StudySummaryVo(10L, "s1", "d","c","sc", monday, monday.plusDays(21), "", org.certis.studyplatform.member.domain.MemberGrade.FRESHMAN, "2024-1", "INPROGRESS", true, java.util.List.of(), 10, 0, org.certis.studyplatform.shared.domain.ResultSubmitStatus.READY),
                    new org.certis.studyplatform.study.domain.vo.StudySummaryVo(11L, "s2", "d","c","sc", monday, monday.plusDays(21), "", org.certis.studyplatform.member.domain.MemberGrade.FRESHMAN, "2024-1", "INPROGRESS", true, java.util.List.of(), 10, 0, org.certis.studyplatform.shared.domain.ResultSubmitStatus.READY)
            );
            when(studyQueryRepository.findActiveStudies(org.springframework.data.domain.Pageable.unpaged()))
                    .thenReturn(org.certis.studyplatform.study.domain.vo.StudySearchResultVo.of(summaries, 2L, 1, 0, 2));
            // 각 요약 id에 대해 생성자 본인으로 반환
            when(studyQueryRepository.findById(10L)).thenReturn(Optional.of(createStudyVo(10L, memberId)));
            when(studyQueryRepository.findById(11L)).thenReturn(Optional.of(createStudyVo(11L, memberId)));

            // 스터디 존재
            Long targetStudyId = 99L;
            when(studyQueryRepository.findByIdAndDeletedAtIsNull(targetStudyId))
                    .thenReturn(Optional.of(createStudyVo(targetStudyId, 2L)));
            when(studyParticipantQueryRepository.findByStudyIdAndMemberId(targetStudyId, memberId))
                    .thenReturn(Optional.empty());

            // When & Then - 생성 2개만으로도 신청 제한
            CreateStudyParticipantCommand participateCommand = new CreateStudyParticipantCommand(targetStudyId, memberId);
            assertThatThrownBy(() -> studyParticipantDomainService.createParticipant(participateCommand))
                    .isInstanceOf(org.certis.studyplatform.exception.DomainException.class)
                    .hasMessage("프로젝트 미진행 시 스터디 2개까지 가능합니다.");
        }
    }

    @Nested
    @DisplayName("프로젝트 생성 vs 참가 신청 제한 비교")
    class ProjectCreationVsParticipationTest {

        @Test
        @DisplayName("진행 중인 프로젝트 1개 상황에서 생성도 불가, 신청도 불가")
        void shouldAllowCreationButRejectParticipation() {
            // Given
            Long memberId = 1L;
            
            // 진행 중인 프로젝트 1개 상황 시뮬레이션
            when(projectParticipantQueryRepository.countActiveProjectsByMemberId(memberId)).thenReturn(1L);
            
            // 프로젝트 생성은 제한 없음
            when(projectCommandRepository.save(any(ProjectVo.class)))
                    .thenReturn(createProjectVo(1L, memberId));
            
            // 프로젝트 참가 신청은 제한 있음
            when(projectQueryRepository.findByIdAndDeletedAtIsNull(anyLong()))
                    .thenReturn(Optional.of(createProjectVo(2L, 2L))); // 다른 사용자가 생성한 프로젝트
            when(projectParticipantQueryRepository.findByProjectIdAndMemberId(anyLong(), eq(memberId)))
                    .thenReturn(Optional.empty());

            // When & Then - 프로젝트 생성은 제한되어야 함 (생성 자체 제한 정책 반영)
            CreateProjectCommand createCommand = createProjectCommand(memberId);
            assertThatThrownBy(() -> projectDomainService.createProject(createCommand))
                    .isInstanceOf(org.certis.studyplatform.exception.DomainException.class)
                    .hasMessageContaining("프로젝트가 1개");

            // When & Then - 프로젝트 참가 신청은 실패해야 함
            CreateProjectParticipantCommand participateCommand = new CreateProjectParticipantCommand(2L, memberId);
            
            assertThatThrownBy(() -> projectParticipantDomainService.createParticipant(participateCommand))
                    .isInstanceOf(org.certis.studyplatform.exception.DomainException.class)
                    .hasMessage("진행 중인 프로젝트가 1개 있으면 추가 신청이 불가합니다.");
        }

        @Test
        @DisplayName("생성한 프로젝트만 1개 있어도 추가 신청 불가(생성+참여 합산)")
        void shouldRejectWhenOneActiveCreatedProjectEvenIfNoJoined() {
            // Given
            Long memberId = 1L;

            // 참여 중 카운트는 0
            when(projectParticipantQueryRepository.countActiveProjectsByMemberId(memberId)).thenReturn(0L);

            // 생성(leader) 기준 진행 중 프로젝트 1건으로 집계되도록 카운트 API 스텁 (도메인 로직은 집계 카운트를 사용)
            when(projectQueryRepository.countActiveProjectsCreatedByMemberId(memberId)).thenReturn(1L);
            when(studyQueryRepository.countActiveStudiesCreatedByMemberId(memberId)).thenReturn(0L);

            // 활성 프로젝트 목록(요약) 1건 반환
            var monday = OffsetDateTime.now().plusWeeks(1).with(DayOfWeek.MONDAY);
            var ps = java.util.List.of(
                    org.certis.studyplatform.project.domain.vo.ProjectSummaryVo.of(
                            20L, "p1", "d","c","sc", monday, monday.plusDays(21),
                            "", org.certis.studyplatform.member.domain.MemberGrade.FRESHMAN,
                            "2024-1", "INPROGRESS", true, null, null, null, null, 10, 0,
                            org.certis.studyplatform.shared.domain.ResultSubmitStatus.READY, java.util.List.of())
            );
            when(projectQueryRepository.findActiveProjects(org.springframework.data.domain.Pageable.unpaged()))
                    .thenReturn(org.certis.studyplatform.project.domain.vo.ProjectSearchResultVo.of(ps, 1L, 1, 0, 1));
            when(projectQueryRepository.findById(20L)).thenReturn(Optional.of(createProjectVo(20L, memberId)));

            // 프로젝트 존재, 중복 없음
            Long targetProjectId = 99L;
            when(projectQueryRepository.findByIdAndDeletedAtIsNull(targetProjectId))
                    .thenReturn(Optional.of(createProjectVo(targetProjectId, 2L)));
            when(projectParticipantQueryRepository.findByProjectIdAndMemberId(targetProjectId, memberId))
                    .thenReturn(Optional.empty());

            // When & Then - 생성 1개만으로도 신청 제한
            CreateProjectParticipantCommand participateCommand = new CreateProjectParticipantCommand(targetProjectId, memberId);
            assertThatThrownBy(() -> projectParticipantDomainService.createParticipant(participateCommand))
                    .isInstanceOf(org.certis.studyplatform.exception.DomainException.class)
                    .hasMessage("진행 중인 프로젝트가 1개 있으면 추가 신청이 불가합니다.");
        }
    }

    @Nested
    @DisplayName("복합 시나리오 테스트")
    class ComplexScenarioTest {

        @Test
        @DisplayName("동일 사용자가 스터디와 프로젝트를 모두 생성 가능하지만 참가 신청은 제한됨")
        void shouldAllowAllCreationsButLimitParticipations() {
            // Given
            Long memberId = 1L;
            
            // 진행 중인 스터디 1개, 프로젝트 1개 상황 시뮬레이션
            when(studyParticipantQueryRepository.countActiveStudiesByMemberId(memberId)).thenReturn(1L);
            when(projectParticipantQueryRepository.countActiveProjectsByMemberId(memberId)).thenReturn(1L);
            
            // 생성은 모두 제한 없음
            when(studyCommandRepository.save(any(StudyVo.class)))
                    .thenReturn(createStudyVo(1L, memberId));
            when(projectCommandRepository.save(any(ProjectVo.class)))
                    .thenReturn(createProjectVo(1L, memberId));
            
            // 참가 신청은 제한 있음
            when(studyQueryRepository.findByIdAndDeletedAtIsNull(anyLong()))
                    .thenReturn(Optional.of(createStudyVo(2L, 2L)));
            when(projectQueryRepository.findByIdAndDeletedAtIsNull(anyLong()))
                    .thenReturn(Optional.of(createProjectVo(2L, 2L)));
            when(studyParticipantQueryRepository.findByStudyIdAndMemberId(anyLong(), eq(memberId)))
                    .thenReturn(Optional.empty());
            when(projectParticipantQueryRepository.findByProjectIdAndMemberId(anyLong(), eq(memberId)))
                    .thenReturn(Optional.empty());

            // When & Then - 생성 자체도 제한됨
            CreateStudyCommand studyCreateCommand = createStudyCommand(memberId);
            assertThatThrownBy(() -> studyDomainService.createStudy(studyCreateCommand))
                    .isInstanceOf(org.certis.studyplatform.exception.DomainException.class);

            CreateProjectCommand projectCreateCommand = createProjectCommand(memberId);
            assertThatThrownBy(() -> projectDomainService.createProject(projectCreateCommand))
                    .isInstanceOf(org.certis.studyplatform.exception.DomainException.class);

            // When & Then - 스터디 참가 신청 실패
            CreateStudyParticipantCommand studyParticipateCommand = new CreateStudyParticipantCommand(2L, memberId);
            assertThatThrownBy(() -> studyParticipantDomainService.createParticipant(studyParticipateCommand))
                    .isInstanceOf(org.certis.studyplatform.exception.DomainException.class)
                    .hasMessage("프로젝트 진행 중에는 스터디 1개까지만 신청할 수 있습니다.");

            // When & Then - 프로젝트 참가 신청 실패
            CreateProjectParticipantCommand projectParticipateCommand = new CreateProjectParticipantCommand(2L, memberId);
            assertThatThrownBy(() -> projectParticipantDomainService.createParticipant(projectParticipateCommand))
                    .isInstanceOf(org.certis.studyplatform.exception.DomainException.class)
                    .hasMessage("진행 중인 프로젝트가 1개 있으면 추가 신청이 불가합니다.");
        }
    }

    // ================================================================
    // Helper Methods
    // ================================================================

    private CreateStudyCommand createStudyCommand(Long creatorId) {
        return CreateStudyCommand.of(
                "테스트 스터디",
                "테스트 설명",
                "테스트 내용",
                "CTF",
                "포너블",
                OffsetDateTime.now().plusWeeks(1).with(DayOfWeek.MONDAY),
                OffsetDateTime.now().plusDays(30),
                null, // githubUrl
                null, // externalUrl
                null, // thumbnailUrl
                null, // attachedFiles
                10, // maxParticipants
                creatorId
        );
    }

    private CreateProjectCommand createProjectCommand(Long creatorId) {
        return CreateProjectCommand.of(
                "테스트 프로젝트",
                "테스트 설명",
                "테스트 내용",
                "CTF",
                "포너블",
                OffsetDateTime.now().plusWeeks(1).with(DayOfWeek.MONDAY),
                OffsetDateTime.now().plusDays(30),
                null, // githubUrl
                null, // externalUrl
                null, // demoUrl
                null, // thumbnailUrl
                null, // attachedFiles
                10, // maxParticipants
                creatorId
        );
    }

    private StudyVo createStudyVo(Long studyId, Long creatorId) {
        OffsetDateTime endDate = OffsetDateTime.now().plusDays(30);
        return StudyVo.createForTest(
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
                null, // creatorProfileImageUrl
                calculateSemester(endDate), // semester
                calculateStatus(endDate), // status
                10,
                0,
                true, // isParticipantable
                java.util.Collections.emptyList(), // attached
                java.util.Collections.emptyList(),
                java.util.Collections.emptyList()
        );
    }

    private ProjectVo createProjectVo(Long projectId, Long creatorId) {
        OffsetDateTime endDate = OffsetDateTime.now().plusDays(30);
        return new ProjectVo(
                projectId,
                "테스트 프로젝트",
                "테스트 설명",
                "테스트 내용",
                "CTF",
                "포너블",
                OffsetDateTime.now().minusDays(1),
                endDate,
                creatorId,
                "생성자",
                "4", // creatorGrade
                calculateSemester(endDate), // semester
                calculateStatus(endDate), // status
                null, // githubUrl
                null, // externalUrl
                null, // demoUrl
                null, // thumbnailUrl
                10,
                0,
                true, // isParticipantable
                java.util.Collections.emptyList(), // attached
                java.util.Collections.emptyList() // meetingSummaryVos
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
}
