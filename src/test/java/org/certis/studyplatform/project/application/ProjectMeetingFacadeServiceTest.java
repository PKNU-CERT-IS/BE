package org.certis.studyplatform.project.application;

import org.certis.studyplatform.member.domain.MemberGrade;
import org.certis.studyplatform.project.application.command.ProjectMeetingCommandService;
import org.certis.studyplatform.project.application.query.ProjectMeetingQueryService;
import org.certis.studyplatform.project.application.query.ProjectParticipantQueryService;
import org.certis.studyplatform.project.domain.ProjectParticipantStatus;
import org.certis.studyplatform.project.domain.vo.*;
import org.certis.studyplatform.project.presentation.dto.request.ProjectMeetingAllRequestDto;
import org.certis.studyplatform.project.presentation.dto.request.ProjectMeetingDetailRequestDto;
import org.certis.studyplatform.project.presentation.dto.response.ProjectMeetingDetailResponseDto;
import org.certis.studyplatform.project.presentation.dto.response.ProjectMeetingSummaryResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Project Meeting Facade Service Test
 *
 * ProjectMeetingFacadeService의 기본 기능 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("프로젝트 회의록 Facade Service 테스트")
class ProjectMeetingFacadeServiceTest {

    @Mock
    private ProjectMeetingCommandService projectMeetingCommandService;

    @Mock
    private ProjectMeetingQueryService projectMeetingQueryService;

    @Mock
    private ProjectParticipantQueryService projectParticipantQueryService;

    private ProjectMeetingFacadeService projectMeetingFacadeService;

    @BeforeEach
    void setUp() {
        projectMeetingFacadeService = new ProjectMeetingFacadeService(
            projectMeetingCommandService, projectMeetingQueryService, projectParticipantQueryService);
    }

    @Test
    @DisplayName("프로젝트 회의록 상세 조회 테스트")
    void getProjectMeetingDetail_ShouldReturnMeetingDetail() {
        // Given
        ProjectMeetingDetailRequestDto request = new ProjectMeetingDetailRequestDto();
        request.setMeetingId(1L);

        // ProjectMeetingDetailVo 생성 (record이므로 생성자 직접 호출)
        ProjectMeetingDetailVo mockMeetingDetailVo = new ProjectMeetingDetailVo(
                1L,                          // id
                1L,                          // projectId
                "킥오프 회의",                 // title
                "회의 내용",                   // content
                4,                           // participantNumber
                1L,                          // writerId
                true,                        // isEditable
                OffsetDateTime.now(),        // createdAt
                OffsetDateTime.now(),        // updatedAt
                List.of()                    // attachedLinks (빈 리스트)
        );

        // ProjectParticipantSummaryVo Mock 데이터 생성
        ProjectParticipantSummaryVo mockParticipant = new ProjectParticipantSummaryVo(
                1L,                          // id
                1L,                          // projectId
                1L,                          // memberId
                "테스트 사용자",               // memberName
                MemberGrade.FRESHMAN,        // memberGrade
                "테스트 프로젝트",             // projectTitle
                ProjectParticipantStatus.APPROVED, // status
                OffsetDateTime.now()         // createdAt
        );
        Page<ProjectParticipantSummaryVo> mockParticipants = new PageImpl<>(List.of(mockParticipant));

        when(projectMeetingQueryService.getProjectMeetingById(any())).thenReturn(mockMeetingDetailVo);
        when(projectParticipantQueryService.getParticipantsByProject(any(), any(), any())).thenReturn(mockParticipants);

        // When
        ProjectMeetingDetailResponseDto result = projectMeetingFacadeService.getProjectMeetingDetail(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("킥오프 회의");
        assertThat(result.getProjectId()).isEqualTo(1L);
        assertThat(result.getParticipantIds()).hasSize(1);
        assertThat(result.getParticipantIds().get(0)).isEqualTo(1L);
        assertThat(result.isEditable()).isTrue();
    }

    @Test
    @DisplayName("프로젝트 회의록 상세 조회 테스트 (링크 포함)")
    void getProjectMeetingDetail_WithLinks_ShouldReturnMeetingDetail() {
        // Given
        ProjectMeetingDetailRequestDto request = new ProjectMeetingDetailRequestDto();
        request.setMeetingId(1L);

        // ProjectMeetingLinkVo 목록 생성
        List<ProjectMeetingLinkVo> mockLinks = List.of(
                new ProjectMeetingLinkVo(
                        1L,                          // id
                        1L,                          // projectId
                        1L,                          // memberId
                        "회의자료",                    // name
                        "https://example.com/doc1",  // attachedUrl
                        OffsetDateTime.now(),        // createdAt
                        OffsetDateTime.now()         // updatedAt
                ),
                new ProjectMeetingLinkVo(
                        2L,                          // id
                        1L,                          // projectId
                        2L,                          // memberId
                        "참고자료",                    // name
                        "https://example.com/doc2",  // attachedUrl
                        OffsetDateTime.now(),        // createdAt
                        OffsetDateTime.now()         // updatedAt
                )
        );

        ProjectMeetingDetailVo mockMeetingDetailVo = new ProjectMeetingDetailVo(
                1L,                          // id
                1L,                          // projectId
                "킥오프 회의",                 // title
                "회의 내용",                   // content
                4,                           // participantNumber
                1L,                          // writerId
                true,                        // isEditable
                OffsetDateTime.now(),        // createdAt
                OffsetDateTime.now(),        // updatedAt
                mockLinks                    // attachedLinks
        );

        // ProjectParticipantSummaryVo Mock 데이터 생성
        ProjectParticipantSummaryVo mockParticipant = new ProjectParticipantSummaryVo(
                1L,                          // id
                1L,                          // projectId
                1L,                          // memberId
                "테스트 사용자",               // memberName
                MemberGrade.FRESHMAN,        // memberGrade
                "테스트 프로젝트",             // projectTitle
                ProjectParticipantStatus.APPROVED, // status
                OffsetDateTime.now()         // createdAt
        );
        Page<ProjectParticipantSummaryVo> mockParticipants = new PageImpl<>(List.of(mockParticipant));

        when(projectMeetingQueryService.getProjectMeetingById(any())).thenReturn(mockMeetingDetailVo);
        when(projectParticipantQueryService.getParticipantsByProject(any(), any(), any())).thenReturn(mockParticipants);

        // When
        ProjectMeetingDetailResponseDto result = projectMeetingFacadeService.getProjectMeetingDetail(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("킥오프 회의");
        assertThat(result.getProjectId()).isEqualTo(1L);
        assertThat(result.getParticipantIds()).hasSize(1);
        assertThat(result.getParticipantIds().get(0)).isEqualTo(1L);
        assertThat(result.isEditable()).isTrue();
        // 링크 관련 추가 검증 가능
    }

    @Test
    @DisplayName("프로젝트 회의록 목록 조회 테스트")
    void getAllProjectMeetings_ShouldReturnMeetingList() {
        // Given
        ProjectMeetingAllRequestDto request = new ProjectMeetingAllRequestDto();
        request.setProjectId(1L);
        Pageable pageable = PageRequest.of(0, 10);

        List<ProjectMeetingSummaryVo> mockMeetings = List.of(
                ProjectMeetingSummaryVo.of(1L, "킥오프 회의", "", 4, "김철수", true, OffsetDateTime.now(), null, null),
                ProjectMeetingSummaryVo.of(2L, "1차 진행상황 회의", "", 4, "이영희", false, OffsetDateTime.now(), null, null),
                ProjectMeetingSummaryVo.of(3L, "중간 점검 회의", "", 3, "박민수", true, OffsetDateTime.now(), null, null),
                ProjectMeetingSummaryVo.of(4L, "최종 발표 준비 회의", "", 4, "정하나", false, OffsetDateTime.now(), null, null)
        );
        Page<ProjectMeetingSummaryVo> mockPage = new PageImpl<>(mockMeetings, pageable, mockMeetings.size());

        // 링크 데이터 생성
        List<ProjectMeetingLinkVo> mockLinks = List.of(
                new ProjectMeetingLinkVo(1L, 1L, 1L, "회의자료1", "https://example.com/doc1",
                        OffsetDateTime.now(), OffsetDateTime.now()),
                new ProjectMeetingLinkVo(2L, 1L, 1L, "회의자료2", "https://example.com/doc2",
                        OffsetDateTime.now(), OffsetDateTime.now()),
                new ProjectMeetingLinkVo(3L, 3L, 2L, "참고자료", "https://example.com/doc3",
                        OffsetDateTime.now(), OffsetDateTime.now())
        );

        // ProjectMeetingPageResultVo 생성
        ProjectMeetingPageResultVo mockPageResult = ProjectMeetingPageResultVo.from(mockPage, mockLinks);

        when(projectMeetingQueryService.getAllProjectMeetings(any())).thenReturn(mockPageResult);

        // When
        Page<ProjectMeetingSummaryResponseDto> result = projectMeetingFacadeService.getAllProjectMeetings(request, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(4);
        assertThat(result.getTotalElements()).isEqualTo(4);

        ProjectMeetingSummaryResponseDto firstMeeting = result.getContent().get(0);
        assertThat(firstMeeting.getId()).isEqualTo(1L);
        assertThat(firstMeeting.getTitle()).isEqualTo("킥오프 회의");
        assertThat(firstMeeting.getParticipantNumber()).isEqualTo(4);
        assertThat(firstMeeting.getCreatorName()).isEqualTo("김철수");
        assertThat(firstMeeting.isEditable()).isTrue();
    }
} 