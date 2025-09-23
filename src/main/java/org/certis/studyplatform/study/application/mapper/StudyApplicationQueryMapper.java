package org.certis.studyplatform.study.application.mapper;

import org.certis.studyplatform.study.presentation.dto.request.StudyAdvancedSearchRequestDto;
import org.certis.studyplatform.study.presentation.dto.request.StudySearchRequestDto;
import org.certis.studyplatform.study.application.object.query.GetAllStudiesQuery;
import org.certis.studyplatform.study.application.object.query.GetStudyByIdQuery;
import org.certis.studyplatform.study.application.object.query.SearchStudiesQuery;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * Study Application Query Mapper
 *
 * Presentation Layer 파라미터를 Application Layer Query 객체로 변환
 */
@Component
public class StudyApplicationQueryMapper {

    /**
     * ID를 GetStudyByIdQuery로 변환
     */
    public GetStudyByIdQuery toGetStudyByIdQuery(Long studyId) {
        return GetStudyByIdQuery.of(studyId);
    }

    /**
     * Pageable을 GetAllStudiesQuery로 변환
     */
    public GetAllStudiesQuery toGetAllStudiesQuery(Pageable pageable) {
        return GetAllStudiesQuery.of(pageable);
    }

    /**
     * 기본 페이징으로 GetAllStudiesQuery 생성
     */
    public GetAllStudiesQuery toGetAllStudiesQuery() {
        Pageable defaultPageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        return GetAllStudiesQuery.of(defaultPageable);
    }


    /**
     * StudyAdvancedSearchRequestDto를 SearchStudiesQuery로 변환 (통합 고급 검색)
     */
    public SearchStudiesQuery toSearchStudiesQuery(StudyAdvancedSearchRequestDto dto, Pageable pageable) {
        return SearchStudiesQuery.ofAdvanced(
            dto.getKeyword(),
            dto.getCategory(),
            dto.getSubcategory(),
            dto.getSemester(),
            dto.getStatus(),
            pageable
        );
    }

    /**
     * 기본 페이징으로 고급 검색 SearchStudiesQuery 생성
     */
    public SearchStudiesQuery toSearchStudiesQuery(StudyAdvancedSearchRequestDto dto) {
        Pageable defaultPageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        return SearchStudiesQuery.ofAdvanced(
            dto.getKeyword(),
            dto.getCategory(),
            dto.getSubcategory(),
            dto.getSemester(),
            dto.getStatus(),
            defaultPageable
        );
    }
}