package org.certis.studyplatform.project.application.mapper;

import org.certis.studyplatform.project.application.object.query.GetAllProjectsQuery;
import org.certis.studyplatform.project.application.object.query.GetProjectByIdQuery;
import org.certis.studyplatform.project.application.object.query.SearchProjectsQuery;
import org.certis.studyplatform.project.presentation.dto.request.ProjectSearchRequestDto;
import org.certis.studyplatform.project.presentation.dto.request.ProjectAdvancedSearchRequestDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * Project Application Query Mapper
 *
 * Presentation Layer 파라미터를 Application Layer Query 객체로 변환
 */
@Component
public class ProjectApplicationQueryMapper {

    /**
     * ID를 GetProjectByIdQuery로 변환
     */
    public GetProjectByIdQuery toGetProjectByIdQuery(Long projectId) {
        return GetProjectByIdQuery.of(projectId);
    }

    /**
     * Pageable을 GetAllProjectsQuery로 변환
     */
    public GetAllProjectsQuery toGetAllProjectsQuery(Pageable pageable) {
        return GetAllProjectsQuery.of(pageable);
    }

    /**
     * 기본 페이징으로 GetAllProjectsQuery 생성
     */
    public GetAllProjectsQuery toGetAllProjectsQuery() {
        Pageable defaultPageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        return GetAllProjectsQuery.of(defaultPageable);
    }

    /**
     * ProjectSearchRequestDto를 SearchProjectsQuery로 변환 (기존 호환성)
     */
    public SearchProjectsQuery toSearchProjectsQuery(ProjectSearchRequestDto dto, Pageable pageable) {
        return SearchProjectsQuery.ofLegacy(
            dto.getKeyword(),
            dto.getCategory(),
            dto.getSubCategory(),
            dto.getSkills(),
            pageable
        );
    }

    /**
     * 기본 페이징으로 레거시 SearchProjectsQuery 생성
     */
    public SearchProjectsQuery toSearchProjectsQuery(ProjectSearchRequestDto dto) {
        Pageable defaultPageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        return SearchProjectsQuery.ofLegacy(
            dto.getKeyword(),
            dto.getCategory(),
            dto.getSubCategory(),
            dto.getSkills(),
            defaultPageable
        );
    }

    /**
     * ProjectAdvancedSearchRequestDto를 SearchProjectsQuery로 변환 (통합 고급 검색)
     */
    public SearchProjectsQuery toSearchProjectsQuery(ProjectAdvancedSearchRequestDto dto, Pageable pageable) {
        return SearchProjectsQuery.ofAdvanced(
            dto.getKeyword(),
            dto.getSemester(),
            dto.getCategory(),
            dto.getSubcategory(),
            dto.getStatus(),
            pageable
        );
    }

    /**
     * 기본 페이징으로 고급 검색 SearchProjectsQuery 생성
     */
    public SearchProjectsQuery toSearchProjectsQuery(ProjectAdvancedSearchRequestDto dto) {
        Pageable defaultPageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        return SearchProjectsQuery.ofAdvanced(
            dto.getKeyword(),
            dto.getSemester(),
            dto.getCategory(),
            dto.getSubcategory(),
            dto.getStatus(),
            defaultPageable
        );
    }
}