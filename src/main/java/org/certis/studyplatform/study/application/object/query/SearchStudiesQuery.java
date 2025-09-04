package org.certis.studyplatform.study.application.object.query;

import org.certis.studyplatform.study.domain.vo.StudyStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Search Studies Query
 *
 * 프로젝트 검색 쿼리 객체
 * 고급 검색 필터 지원 (keyword, semester, category, subcategory, status)
 */
public record SearchStudiesQuery(
    String keyword,
    String category,
    String subCategory,
    StudyStatus status,
    Pageable pageable
) {
    public static SearchStudiesQuery of(
        String keyword,
        String category,
        String subCategory,
        StudyStatus status,
        Pageable pageable
    ) {
        return new SearchStudiesQuery(keyword, category, subCategory, status, pageable);
    }

    public static SearchStudiesQuery ofKeyword(String keyword, Pageable pageable) {
        return new SearchStudiesQuery(keyword, null, null, null,  pageable);
    }

    /**
     * 고급 검색용 팩토리 메서드
     */
    public static SearchStudiesQuery ofAdvanced(
        String keyword,
        String category,
        String subcategory,
        StudyStatus status,
        Pageable pageable
    ) {
        return new SearchStudiesQuery(keyword, category, subcategory, status, null);
    }

    /**
     * 검색 조건이 비어있는지 확인
     */
    public boolean isEmpty() {
        return (keyword == null || keyword.trim().isEmpty()) &&
               (category == null || category.trim().isEmpty()) &&
               (subCategory == null || subCategory.trim().isEmpty()) &&
               (status == null);
    }

    /**
     * 키워드 검색이 있는지 확인
     */
    public boolean hasKeyword() {
        return keyword != null && !keyword.trim().isEmpty();
    }

    /**
     * 카테고리 필터가 있는지 확인
     */
    public boolean hasCategory() {
        return category != null && !category.trim().isEmpty();
    }

    /**
     * 서브카테고리 필터가 있는지 확인
     */
    public boolean hasSubCategory() {
        return subCategory != null && !subCategory.trim().isEmpty();
    }

    /**
     * 상태 필터가 있는지 확인
     */
    public boolean hasStatus() {
        return status != null;
    }

}