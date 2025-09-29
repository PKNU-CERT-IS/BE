package org.certis.studyplatform.project.application.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.certis.studyplatform.project.application.object.query.GetAllProjectsQuery;
import org.certis.studyplatform.project.application.object.query.GetProjectByIdQuery;
import org.certis.studyplatform.project.application.object.query.SearchProjectsQuery;
import org.certis.studyplatform.project.domain.service.ProjectDomainService;
import org.certis.studyplatform.project.domain.vo.ProjectSummaryVo;
import org.certis.studyplatform.project.domain.vo.ProjectVo;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Project Query Service
 *
 * Clean Architecture Application Layer
 * 프로젝트 읽기 작업 처리 (CQRS Query Side)
 *
 * Query 객체를 Domain Service로 전달하여 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectQueryService {

    private final ProjectDomainService projectDomainService;

    /**
     * 프로젝트 상세 조회
     */
    @Transactional(readOnly = true)
    public ProjectVo getProjectById(GetProjectByIdQuery query) {
        log.info("Query: Getting project by ID - {}", query.id());

        // Query 객체를 Domain Service로 전달
        ProjectVo projectVo = projectDomainService.getProjectById(query);

        log.info("Query: Project retrieved successfully - ID: {}", projectVo.id());
        return projectVo;
    }

    /**
     * 전체 프로젝트 조회
     */
    @Transactional(readOnly = true)
    public Page<ProjectSummaryVo> getAllProjects(GetAllProjectsQuery query) {
        log.info("Query: Getting all projects with pagination");

        // Query 객체를 Domain Service로 전달
        Page<ProjectSummaryVo> projects = projectDomainService.getAllProjects(query);

        log.info("Query: All projects retrieved - found {} projects", projects.getTotalElements());
        return projects;
    }

    /**
     * 프로젝트 검색
     */
    @Transactional(readOnly = true)
    public Page<ProjectSummaryVo> searchProjects(SearchProjectsQuery query) {
        log.info("Query: Searching projects - keyword: {}, category: {}",
                query.keyword(), query.category());

        // Query 객체를 Domain Service로 전달
        Page<ProjectSummaryVo> projects = projectDomainService.searchProjectsByCriteria(query);

        log.info("Query: Project search completed - found {} projects", projects.getTotalElements());
        return projects;
    }

    @Transactional(readOnly = true)
    public java.util.List<org.certis.studyplatform.project.domain.vo.ProjectEndSubmissionInfoVo> getEndSubmissionsInProgress() {
        return projectDomainService.getEndSubmissionsInProgress();
    }
}