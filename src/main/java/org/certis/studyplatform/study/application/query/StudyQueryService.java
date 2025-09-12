package org.certis.studyplatform.study.application.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.study.domain.service.StudyDomainService;
import org.certis.studyplatform.study.domain.vo.StudySummaryVo;
import org.certis.studyplatform.study.domain.vo.StudyVo;
import org.certis.studyplatform.study.application.object.query.GetAllStudiesQuery;
import org.certis.studyplatform.study.application.object.query.GetStudyByIdQuery;
import org.certis.studyplatform.study.application.object.query.SearchStudiesQuery;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Study Query Service
 *
 * Clean Architecture Application Layer
 * 프로젝트 읽기 작업 처리 (CQRS Query Side)
 *
 * Query 객체를 Domain Service로 전달하여 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StudyQueryService {

    private final StudyDomainService studyDomainService;

    /**
     * 프로젝트 상세 조회
     */
    @Transactional(readOnly = true)
    public StudyVo getStudyById(GetStudyByIdQuery query) {
        log.info("Query: Getting study by ID - {}", query.id());

        // Query 객체를 Domain Service로 전달
        StudyVo studyVo = studyDomainService.getStudyById(query);

        log.info("Query: Study retrieved successfully - ID: {}", studyVo.id());
        return studyVo;
    }

    /**
     * 전체 프로젝트 조회
     */
    @Transactional(readOnly = true)
    public Page<StudySummaryVo> getAllStudies(GetAllStudiesQuery query) {
        log.info("Query: Getting all studies with pagination");

        // Query 객체를 Domain Service로 전달
        Page<StudySummaryVo> studies = studyDomainService.getAllStudies(query);

        log.info("Query: All studies retrieved - found {} studies", studies.getTotalElements());
        return studies;
    }

    /**
     * 프로젝트 검색
     */
    @Transactional(readOnly = true)
    public Page<StudySummaryVo> searchStudies(SearchStudiesQuery query) {
        log.info("Query: Searching studies - keyword: {}, category: {}",
                query.keyword(), query.category());

        // Query 객체를 Domain Service로 전달
        Page<StudySummaryVo> studies = studyDomainService.searchStudiesByCriteria(query);

        log.info("Query: Study search completed - found {} studies", studies.getTotalElements());
        return studies;
    }
}