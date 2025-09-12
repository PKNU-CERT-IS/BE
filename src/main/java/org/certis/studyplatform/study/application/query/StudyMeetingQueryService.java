package org.certis.studyplatform.study.application.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.study.domain.service.StudyMeetingDomainService;
import org.certis.studyplatform.study.domain.vo.StudyMeetingDetailVo;
import org.certis.studyplatform.study.domain.vo.StudyMeetingPageResultVo;
import org.certis.studyplatform.study.application.object.query.GetAllStudyMeetingsQuery;
import org.certis.studyplatform.study.application.object.query.GetStudyMeetingByIdQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Study Meeting Query Service
 *
 * Clean Architecture Application Layer
 * 프로젝트 회의록 읽기 작업 처리 (CQRS Query Side)
 *
 * Query 객체를 Domain Service로 전달하여 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StudyMeetingQueryService {

    private final StudyMeetingDomainService studyMeetingDomainService;

    /**
     * 프로젝트 회의록 상세 조회
     */
    @Transactional(readOnly = true)
    public StudyMeetingDetailVo getStudyMeetingById(GetStudyMeetingByIdQuery query) {
        log.info("MeetingQuery: Getting study meeting by ID - {}", query.meetingId());

        // Query 객체를 Domain Service로 전달
        StudyMeetingDetailVo meetingVo = studyMeetingDomainService.getStudyMeetingById(query);

        log.info("MeetingQuery: Study meeting retrieved successfully - ID: {}", meetingVo.id());
        return meetingVo;
    }

    /**
     * 프로젝트 회의록 전체 목록 조회
     */
    @Transactional(readOnly = true)
    public StudyMeetingPageResultVo getAllStudyMeetings(GetAllStudyMeetingsQuery query) {
        log.info("MeetingQuery: Getting all study meetings - studyId: {}, page: {}, size: {}",
                query.studyId(), query.pageable().getPageNumber(), query.pageable().getPageSize());

        // Query 객체를 Domain Service로 전달
        StudyMeetingPageResultVo meetings = studyMeetingDomainService.getAllStudyMeetings(query);

        return meetings;
    }
} 