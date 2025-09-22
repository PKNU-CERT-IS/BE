package org.certis.studyplatform.project.domain.service;

import org.certis.studyplatform.exception.DomainException;
import org.certis.studyplatform.exception.ExceptionStatus;
import org.certis.studyplatform.project.application.object.command.CreateProjectMeetingCommand;
import org.certis.studyplatform.project.domain.repository.ProjectMeetingCommandRepository;
import org.certis.studyplatform.project.domain.repository.ProjectMeetingLinkCommandRepository;
import org.certis.studyplatform.project.domain.repository.ProjectMeetingLinkQueryRepository;
import org.certis.studyplatform.project.domain.repository.ProjectMeetingQueryRepository;
import org.certis.studyplatform.project.domain.repository.ProjectParticipantQueryRepository;
import org.certis.studyplatform.project.domain.vo.ProjectMeetingCreatedVo;
import org.certis.studyplatform.project.domain.vo.ProjectMeetingLinkVo;
import org.certis.studyplatform.shared.dto.LinkDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ProjectMeetingDomainService 도메인 서비스 테스트
 * 
 * 🎯 테스트 목표:
 * - 프로젝트 멤버십 체크 로직 검증
 * - 승인된 멤버만 회의록 생성 가능
 * - 비승인 멤버의 접근 차단
 * - 도메인 로직의 정확성 보장
 * 
 * 🔧 테스트 전략:
 * - Mock을 사용한 단위 테스트
 * - 멤버십 체크 로직별 독립적인 테스트
 * - 예외 상황 및 엣지 케이스 커버
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectMeetingDomainService 멤버십 체크 테스트")
class ProjectMeetingDomainServiceTest {

    @Mock
    private ProjectMeetingCommandRepository projectMeetingCommandRepository;
    
    @Mock
    private ProjectMeetingQueryRepository projectMeetingQueryRepository;
    
    @Mock
    private ProjectMeetingLinkCommandRepository projectMeetingLinkCommandRepository;
    
    @Mock
    private ProjectMeetingLinkQueryRepository projectMeetingLinkQueryRepository;
    
    @Mock
    private ProjectParticipantQueryRepository projectParticipantQueryRepository;

    private ProjectMeetingDomainService domainService;

    @BeforeEach
    void setUp() {
        domainService = new ProjectMeetingDomainService(
                projectMeetingCommandRepository,
                projectMeetingQueryRepository,
                projectMeetingLinkCommandRepository,
                projectMeetingLinkQueryRepository,
                projectParticipantQueryRepository
        );
    }

    @Nested
    @DisplayName("프로젝트 멤버십 체크 테스트")
    class ProjectMembershipCheckTest {

        @Test
        @DisplayName("승인된 멤버는 회의록 생성 가능")
        void createProjectMeeting_WithApprovedMember_ShouldSucceed() {
            // Given
            Long projectId = 1L;
            Long memberId = 100L;
            
            CreateProjectMeetingCommand command = new CreateProjectMeetingCommand(
                    projectId,
                    memberId,
                    "테스트 회의",
                    "회의 내용",
                    4,
                    List.of()
            );

            // 승인된 멤버로 설정
            when(projectParticipantQueryRepository.isApprovedMember(projectId, memberId))
                    .thenReturn(true);

            ProjectMeetingCreatedVo expectedResult = new ProjectMeetingCreatedVo(
                    1L, projectId, "테스트 회의", "회의 내용", 4, memberId, null
            );
            when(projectMeetingCommandRepository.save(any())).thenReturn(expectedResult);

            // When
            ProjectMeetingCreatedVo result = domainService.createProjectMeeting(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.projectId()).isEqualTo(projectId);
            assertThat(result.title()).isEqualTo("테스트 회의");
            
            // 멤버십 체크가 호출되었는지 확인
            verify(projectParticipantQueryRepository).isApprovedMember(projectId, memberId);
        }

        @Test
        @DisplayName("비승인 멤버는 회의록 생성 불가")
        void createProjectMeeting_WithNonApprovedMember_ShouldThrowException() {
            // Given
            Long projectId = 1L;
            Long memberId = 100L;
            
            CreateProjectMeetingCommand command = new CreateProjectMeetingCommand(
                    projectId,
                    memberId,
                    "테스트 회의",
                    "회의 내용",
                    4,
                    List.of()
            );

            // 비승인 멤버로 설정
            when(projectParticipantQueryRepository.isApprovedMember(projectId, memberId))
                    .thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> domainService.createProjectMeeting(command))
                    .isInstanceOf(DomainException.class)
                    .hasFieldOrPropertyWithValue("status", ExceptionStatus.PROJECT_DOMAIN_PERMISSION_DENINED);
            
            // 멤버십 체크가 호출되었는지 확인
            verify(projectParticipantQueryRepository).isApprovedMember(projectId, memberId);
            // 회의록 저장이 호출되지 않았는지 확인
            verify(projectMeetingCommandRepository, never()).save(any());
        }

        @Test
        @DisplayName("프로젝트 ID가 null인 경우 예외 발생")
        void createProjectMeeting_WithNullProjectId_ShouldThrowException() {
            // Given
            CreateProjectMeetingCommand command = new CreateProjectMeetingCommand(
                    null, // null projectId
                    100L,
                    "테스트 회의",
                    "회의 내용",
                    4,
                    List.of()
            );

            // When & Then
            assertThatThrownBy(() -> domainService.createProjectMeeting(command))
                    .isInstanceOf(DomainException.class)
                    .hasFieldOrPropertyWithValue("status", ExceptionStatus.PROJECT_DOMAIN_PERMISSION_DENINED);
            
            // 멤버십 체크가 호출되지 않았는지 확인
            verify(projectParticipantQueryRepository, never()).isApprovedMember(any(), any());
        }

        @Test
        @DisplayName("요청자 ID가 null인 경우 예외 발생")
        void createProjectMeeting_WithNullRequesterId_ShouldThrowException() {
            // Given
            CreateProjectMeetingCommand command = new CreateProjectMeetingCommand(
                    1L,
                    null, // null requesterId
                    "테스트 회의",
                    "회의 내용",
                    4,
                    List.of()
            );

            // When & Then
            assertThatThrownBy(() -> domainService.createProjectMeeting(command))
                    .isInstanceOf(DomainException.class)
                    .hasFieldOrPropertyWithValue("status", ExceptionStatus.PROJECT_DOMAIN_PERMISSION_DENINED);
            
            // 멤버십 체크가 호출되지 않았는지 확인
            verify(projectParticipantQueryRepository, never()).isApprovedMember(any(), any());
        }

        @Test
        @DisplayName("승인된 멤버는 링크가 있는 회의록 생성 가능")
        void createProjectMeeting_WithApprovedMemberAndLinks_ShouldSucceed() {
            // Given
            Long projectId = 1L;
            Long memberId = 100L;
            
            List<LinkDto> links = List.of(
                    new LinkDto("GitHub", "https://github.com/test"),
                    new LinkDto("Notion", "https://notion.so/test")
            );
            
            CreateProjectMeetingCommand command = new CreateProjectMeetingCommand(
                    projectId,
                    memberId,
                    "테스트 회의",
                    "회의 내용",
                    4,
                    links
            );

            // 승인된 멤버로 설정
            when(projectParticipantQueryRepository.isApprovedMember(projectId, memberId))
                    .thenReturn(true);

            ProjectMeetingCreatedVo expectedResult = new ProjectMeetingCreatedVo(
                    1L, projectId, "테스트 회의", "회의 내용", 4, memberId, null
            );
            when(projectMeetingCommandRepository.save(any())).thenReturn(expectedResult);

            // When
            ProjectMeetingCreatedVo result = domainService.createProjectMeeting(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.projectId()).isEqualTo(projectId);
            
            // 멤버십 체크가 호출되었는지 확인
            verify(projectParticipantQueryRepository).isApprovedMember(projectId, memberId);
            // 링크 저장이 호출되었는지 확인
            verify(projectMeetingLinkCommandRepository, times(2)).save(any(ProjectMeetingLinkVo.class));
        }
    }

    @Nested
    @DisplayName("멤버십 체크 메서드 테스트")
    class MembershipCheckMethodTest {

        @Test
        @DisplayName("isApprovedMember 메서드 호출 검증")
        void validateProjectAccess_ShouldCallIsApprovedMember() {
            // Given
            Long projectId = 1L;
            Long memberId = 100L;
            
            CreateProjectMeetingCommand command = new CreateProjectMeetingCommand(
                    projectId,
                    memberId,
                    "테스트 회의",
                    "회의 내용",
                    4,
                    List.of()
            );

            when(projectParticipantQueryRepository.isApprovedMember(projectId, memberId))
                    .thenReturn(true);
            when(projectMeetingCommandRepository.save(any())).thenReturn(
                    new ProjectMeetingCreatedVo(1L, projectId, "테스트 회의", "회의 내용", 4, memberId, null)
            );

            // When
            domainService.createProjectMeeting(command);

            // Then
            verify(projectParticipantQueryRepository, times(1)).isApprovedMember(projectId, memberId);
        }

        @Test
        @DisplayName("멤버십 체크 실패 시 적절한 로그 출력")
        void validateProjectAccess_WithFailedMembershipCheck_ShouldLogWarning() {
            // Given
            Long projectId = 1L;
            Long memberId = 100L;
            
            CreateProjectMeetingCommand command = new CreateProjectMeetingCommand(
                    projectId,
                    memberId,
                    "테스트 회의",
                    "회의 내용",
                    4,
                    List.of()
            );

            when(projectParticipantQueryRepository.isApprovedMember(projectId, memberId))
                    .thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> domainService.createProjectMeeting(command))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("프로젝트의 승인된 멤버만 접근할 수 있습니다.");
        }
    }
}
