package org.certis.studyplatform.project.application.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.project.application.object.query.GetAllProjectMeetingsQuery;
import org.certis.studyplatform.project.application.object.query.GetProjectMeetingByIdQuery;
import org.certis.studyplatform.project.domain.service.ProjectMeetingDomainService;
import org.certis.studyplatform.project.domain.vo.ProjectMeetingDetailVo;
import org.certis.studyplatform.project.domain.vo.ProjectMeetingPageResultVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Project Meeting Query Service
 *
 * Clean Architecture Application Layer
 * 프로젝트 회의록 읽기 작업 처리 (CQRS Query Side)
 *
 * Query 객체를 Domain Service로 전달하여 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectMeetingQueryService {

    private final ProjectMeetingDomainService projectMeetingDomainService;

    /**
     * 프로젝트 회의록 상세 조회
     */
    @Transactional(readOnly = true)
    public ProjectMeetingDetailVo getProjectMeetingById(GetProjectMeetingByIdQuery query) {
        log.info("MeetingQuery: Getting project meeting by ID - {}", query.meetingId());

        // Query 객체를 Domain Service로 전달
        ProjectMeetingDetailVo meetingVo = projectMeetingDomainService.getProjectMeetingById(query);

        log.info("MeetingQuery: Project meeting retrieved successfully - ID: {}", meetingVo.id());
        return meetingVo;
    }

    /**
     * 프로젝트 회의록 전체 목록 조회
     */
    @Transactional(readOnly = true)
    public ProjectMeetingPageResultVo getAllProjectMeetings(GetAllProjectMeetingsQuery query) {
        log.info("MeetingQuery: Getting all project meetings - projectId: {}, page: {}, size: {}", 
                query.projectId(), query.pageable().getPageNumber(), query.pageable().getPageSize());

        // Query 객체를 Domain Service로 전달
        ProjectMeetingPageResultVo meetings = projectMeetingDomainService.getAllProjectMeetings(query);

        return meetings;
    }
} 