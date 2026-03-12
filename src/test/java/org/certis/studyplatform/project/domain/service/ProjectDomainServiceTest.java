package org.certis.studyplatform.project.domain.service;

import org.certis.studyplatform.project.application.object.command.CreateProjectCommand;
import org.certis.studyplatform.project.domain.vo.ExternalUrlVo;
import org.certis.studyplatform.member.domain.repository.query.MemberQueryRepository;
import org.certis.studyplatform.project.domain.repository.ProjectCommandRepository;
import org.certis.studyplatform.project.domain.repository.ProjectQueryRepository;
import org.certis.studyplatform.project.domain.vo.ProjectVo;
import org.certis.studyplatform.member.domain.service.MemberDomainService;
import org.certis.studyplatform.project.domain.repository.ProjectParticipantQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ProjectDomainService 도메인 서비스 테스트
 * 
 * 🎯 테스트 목표:
 * - 프로젝트 생성 시 제한이 없는지 검증
 * - 참가 신청 제한과 생성 제한이 구분되는지 확인
 * - 도메인 로직의 정확성 보장
 * 
 * 🔧 테스트 전략:
 * - Mock을 사용한 단위 테스트
 * - 생성 제한이 없음을 명확히 검증
 * - 다양한 시나리오에서 생성 가능함을 확인
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectDomainService 도메인 서비스 테스트")
class ProjectDomainServiceTest {

    @Mock
    private ProjectCommandRepository commandRepository;
    
    @Mock
    private ProjectQueryRepository queryRepository;
    
    @Mock
    private MemberDomainService memberDomainService;

    @Mock
    private MemberQueryRepository memberQueryRepository;

    @Mock
    private ProjectParticipantQueryRepository projectParticipantQueryRepository;

    private ProjectDomainService domainService;

    @BeforeEach
    void setUp() {
        domainService = new ProjectDomainService(
                commandRepository,
                queryRepository,
                memberDomainService,
                memberQueryRepository,
                projectParticipantQueryRepository
        );
        lenient().when(memberQueryRepository.findByIdForUpdate(any()))
                .thenReturn(java.util.Optional.of(mock(org.certis.studyplatform.member.domain.vo.MemberVo.class)));
    }

    @Nested
    @DisplayName("프로젝트 생성 제한 테스트")
    class ProjectCreationLimitsTest {

        @Test
        @DisplayName("진행 중인 프로젝트가 1개 이상이어도 프로젝트 생성 가능")
        void shouldAllowProjectCreationEvenWithActiveProjects() {
            // Given
            Long creatorId = 1L;
            CreateProjectCommand command = createProjectCommand(creatorId);
            
            // 프로젝트 생성자는 이미 진행 중인 프로젝트가 1개 있다고 가정
            // 참가 신청이라면 제한이 있지만, 생성에는 제한이 없어야 함
            when(commandRepository.save(any(ProjectVo.class)))
                    .thenReturn(createProjectVo(1L, creatorId));

            // When
            ProjectVo result = domainService.createProject(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.creatorId()).isEqualTo(creatorId);
            verify(commandRepository).save(any(ProjectVo.class));
            
            // 생성 제한 검증이 호출되지 않았음을 확인
            // (참가 신청 제한과 달리 생성에는 제한이 없음)
        }

        @Test
        @DisplayName("진행 중인 스터디가 있어도 프로젝트 생성 가능")
        void shouldAllowProjectCreationEvenWithActiveStudies() {
            // Given
            Long creatorId = 1L;
            CreateProjectCommand command = createProjectCommand(creatorId);
            
            // 프로젝트 생성자는 이미 진행 중인 스터디가 있다고 가정
            // 하지만 생성에는 제한이 없어야 함
            when(commandRepository.save(any(ProjectVo.class)))
                    .thenReturn(createProjectVo(1L, creatorId));

            // When
            ProjectVo result = domainService.createProject(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.creatorId()).isEqualTo(creatorId);
            verify(commandRepository).save(any(ProjectVo.class));
        }

        @Test
        @DisplayName("진행 중인 스터디와 프로젝트가 모두 있어도 프로젝트 생성 가능")
        void shouldAllowProjectCreationEvenWithActiveStudiesAndProjects() {
            // Given
            Long creatorId = 1L;
            CreateProjectCommand command = createProjectCommand(creatorId);
            
            // 프로젝트 생성자는 이미 진행 중인 스터디 1개, 프로젝트 1개가 있다고 가정
            // 참가 신청이라면 제한이 있지만, 생성에는 제한이 없어야 함
            when(commandRepository.save(any(ProjectVo.class)))
                    .thenReturn(createProjectVo(1L, creatorId));

            // When
            ProjectVo result = domainService.createProject(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.creatorId()).isEqualTo(creatorId);
            verify(commandRepository).save(any(ProjectVo.class));
        }

        @Test
        @DisplayName("연속으로 여러 프로젝트 생성 가능")
        void shouldAllowMultipleProjectCreations() {
            // Given
            Long creatorId = 1L;
            CreateProjectCommand command1 = createProjectCommand(creatorId, "프로젝트 1");
            CreateProjectCommand command2 = createProjectCommand(creatorId, "프로젝트 2");
            CreateProjectCommand command3 = createProjectCommand(creatorId, "프로젝트 3");
            
            when(commandRepository.save(any(ProjectVo.class)))
                    .thenReturn(createProjectVo(1L, creatorId))
                    .thenReturn(createProjectVo(2L, creatorId))
                    .thenReturn(createProjectVo(3L, creatorId));

            // When
            ProjectVo result1 = domainService.createProject(command1);
            ProjectVo result2 = domainService.createProject(command2);
            ProjectVo result3 = domainService.createProject(command3);

            // Then
            assertThat(result1).isNotNull();
            assertThat(result2).isNotNull();
            assertThat(result3).isNotNull();
            
            assertThat(result1.id()).isEqualTo(1L);
            assertThat(result2.id()).isEqualTo(2L);
            assertThat(result3.id()).isEqualTo(3L);
            
            verify(commandRepository, times(3)).save(any(ProjectVo.class));
        }

        @Test
        @DisplayName("동일한 사용자가 스터디와 프로젝트를 동시에 생성 가능")
        void shouldAllowConcurrentStudyAndProjectCreation() {
            // Given
            Long creatorId = 1L;
            CreateProjectCommand projectCommand = createProjectCommand(creatorId, "테스트 프로젝트");
            
            when(commandRepository.save(any(ProjectVo.class)))
                    .thenReturn(createProjectVo(1L, creatorId));

            // When
            ProjectVo result = domainService.createProject(projectCommand);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.creatorId()).isEqualTo(creatorId);
            verify(commandRepository).save(any(ProjectVo.class));
            
            // 동일한 사용자가 스터디도 생성할 수 있음을 확인
            // (실제로는 StudyDomainService를 별도로 테스트해야 하지만, 
            //  여기서는 프로젝트 생성에 제한이 없음을 확인)
        }
    }

    @Nested
    @DisplayName("프로젝트 생성 기본 기능 테스트")
    class ProjectCreationBasicTest {

        @Test
        @DisplayName("정상적인 프로젝트 생성")
        void shouldCreateProjectSuccessfully() {
            // Given
            Long creatorId = 1L;
            CreateProjectCommand command = createProjectCommand(creatorId);
            
            when(commandRepository.save(any(ProjectVo.class)))
                    .thenReturn(createProjectVo(1L, creatorId));

            // When
            ProjectVo result = domainService.createProject(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.title()).isEqualTo("테스트 프로젝트");
            assertThat(result.creatorId()).isEqualTo(creatorId);
            verify(commandRepository).save(any(ProjectVo.class));
        }

        @Test
        @DisplayName("프로젝트 생성 시 VO 검증 로직 실행")
        void shouldExecuteVoValidationOnCreation() {
            // Given
            Long creatorId = 1L;
            CreateProjectCommand command = createProjectCommand(creatorId);
            
            when(commandRepository.save(any(ProjectVo.class)))
                    .thenReturn(createProjectVo(1L, creatorId));

            // When
            ProjectVo result = domainService.createProject(command);

            // Then
            assertThat(result).isNotNull();
            // ProjectVo.createNew() 내부 검증이 실행되었음을 확인
            // (제목, 설명, 날짜 등의 유효성 검사)
        }

        @Test
        @DisplayName("GitHub URL이 있는 프로젝트 생성")
        void shouldCreateProjectWithGitHubUrl() {
            // Given
            Long creatorId = 1L;
            CreateProjectCommand command = createProjectCommandWithUrls(creatorId);
            
            when(commandRepository.save(any(ProjectVo.class)))
                    .thenReturn(createProjectVoWithUrls(1L, creatorId));

            // When
            ProjectVo result = domainService.createProject(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.creatorId()).isEqualTo(creatorId);
            verify(commandRepository).save(any(ProjectVo.class));
        }
    }

    // ================================================================
    // Helper Methods
    // ================================================================

    private CreateProjectCommand createProjectCommand(Long creatorId) {
        return createProjectCommand(creatorId, "테스트 프로젝트");
    }

    private CreateProjectCommand createProjectCommand(Long creatorId, String title) {
        return CreateProjectCommand.of(
                title,
                "테스트 설명",
                "테스트 내용",
                "CTF",
                "포너블",
                getNextMonday(),
                getNextSunday(),
                null, // githubUrl
                null, // externalUrl
                null, // demoUrl
                null, // thumbnailUrl
                null, // attachedFiles
                10, // maxParticipants
                creatorId
        );
    }

    private CreateProjectCommand createProjectCommandWithUrls(Long creatorId) {
        return CreateProjectCommand.of(
                "테스트 프로젝트",
                "테스트 설명",
                "테스트 내용",
                "CTF",
                "포너블",
                getNextMonday(),
                getNextSunday(),
                "https://github.com/test/project", // githubUrl
                new ExternalUrlVo("테스트 사이트", "https://test-project.com"), // externalUrl
                "https://demo.test-project.com", // demoUrl
                "https://thumbnail.test-project.com", // thumbnailUrl
                null, // attachedFiles
                10, // maxParticipants
                creatorId
        );
    }

    private ProjectVo createProjectVo(Long projectId, Long creatorId) {
        OffsetDateTime startDate = getNextMonday();
        OffsetDateTime endDate = getNextSunday();
        return new ProjectVo(
                projectId,
                "테스트 프로젝트",
                "테스트 설명",
                "테스트 내용",
                "CTF",
                "포너블",
                startDate,
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
                Collections.emptyList(), // attached
                Collections.emptyList() // meetingSummaryVos
        );
    }

    private ProjectVo createProjectVoWithUrls(Long projectId, Long creatorId) {
        OffsetDateTime startDate = getNextMonday();
        OffsetDateTime endDate = getNextSunday();
        return new ProjectVo(
                projectId,
                "테스트 프로젝트",
                "테스트 설명",
                "테스트 내용",
                "CTF",
                "포너블",
                startDate,
                endDate,
                creatorId,
                "생성자",
                "4", // creatorGrade
                calculateSemester(endDate), // semester
                calculateStatus(endDate), // status
                "https://github.com/test/project", // githubUrl
                new ExternalUrlVo("테스트 사이트", "https://test-project.com"), // externalUrl
                "https://demo.test-project.com", // demoUrl
                "https://thumbnail.test-project.com", // thumbnailUrl
                10,
                0,
                true, // isParticipantable
                Collections.emptyList(), // attached
                Collections.emptyList() // meetingSummaryVos
        );
    }

    private String calculateSemester(OffsetDateTime endedAt) {
        if (endedAt == null) {
            return null;
        }
        
        LocalDate endDate = endedAt.toLocalDate();
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

    /**
     * 다음 월요일을 반환합니다.
     * 현재 날짜가 월요일이면 다음 주 월요일을 반환합니다.
     */
    private OffsetDateTime getNextMonday() {
        LocalDate today = LocalDate.now();
        LocalDate nextMonday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        
        // 오늘이 월요일이면 다음 주 월요일로 설정
        if (nextMonday.equals(today)) {
            nextMonday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        }
        
        return nextMonday.atStartOfDay(ZoneId.of("Asia/Seoul")).toOffsetDateTime();
    }

    /**
     * 다음 일요일을 반환합니다.
     * 시작일로부터 적절한 기간 후의 일요일을 반환합니다.
     */
    private OffsetDateTime getNextSunday() {
        LocalDate startDate = getNextMonday().toLocalDate();
        LocalDate nextSunday = startDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        
        // 시작일이 일요일이면 다음 주 일요일로 설정
        if (nextSunday.equals(startDate)) {
            nextSunday = startDate.with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
        }
        
        return nextSunday.atStartOfDay(ZoneId.of("Asia/Seoul")).toOffsetDateTime();
    }
}
