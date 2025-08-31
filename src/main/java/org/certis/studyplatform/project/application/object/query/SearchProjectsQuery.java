package org.certis.studyplatform.project.application.object.query;

import org.springframework.data.domain.Pageable;
import java.util.List;

/**
 * Search Projects Query
 *
 * 프로젝트 검색 쿼리 객체
 * 고급 검색 필터 지원 (keyword, semester, category, subcategory, status)
 */
public record SearchProjectsQuery(
    String keyword,
    String semester,
    String category,
    String subCategory,
    String status,
    List<String> techStack,
    Pageable pageable
) {
    public static SearchProjectsQuery of(
        String keyword,
        String semester,
        String category,
        String subCategory,
        String status,
        List<String> techStack,
        Pageable pageable
    ) {
        return new SearchProjectsQuery(keyword, semester, category, subCategory, status, techStack, pageable);
    }

    public static SearchProjectsQuery ofKeyword(String keyword, Pageable pageable) {
        return new SearchProjectsQuery(keyword, null, null, null, null, null, pageable);
    }

    /**
     * 기존 호환성을 위한 팩토리 메서드
     */
    public static SearchProjectsQuery ofLegacy(
        String keyword,
        String category,
        String subCategory,
        List<String> techStack,
        Pageable pageable
    ) {
        return new SearchProjectsQuery(keyword, null, category, subCategory, null, techStack, pageable);
    }

    /**
     * 고급 검색용 팩토리 메서드
     */
    public static SearchProjectsQuery ofAdvanced(
        String keyword,
        String semester,
        String category,
        String subcategory,
        String status,
        Pageable pageable
    ) {
        return new SearchProjectsQuery(keyword, semester, category, subcategory, status, null, pageable);
    }

    /**
     * 검색 조건이 비어있는지 확인
     */
    public boolean isEmpty() {
        return (keyword == null || keyword.trim().isEmpty()) &&
               (semester == null || semester.trim().isEmpty()) &&
               (category == null || category.trim().isEmpty()) &&
               (subCategory == null || subCategory.trim().isEmpty()) &&
               (status == null || status.trim().isEmpty()) &&
               (techStack == null || techStack.isEmpty());
    }

    /**
     * 키워드 검색이 있는지 확인
     */
    public boolean hasKeyword() {
        return keyword != null && !keyword.trim().isEmpty();
    }

    /**
     * 학기 필터가 있는지 확인
     */
    public boolean hasSemester() {
        return semester != null && !semester.trim().isEmpty();
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
        return status != null && !status.trim().isEmpty();
    }

    /**
     * 기술스택 필터가 있는지 확인
     */
    public boolean hasTechStack() {
        return techStack != null && !techStack.isEmpty();
    }
}