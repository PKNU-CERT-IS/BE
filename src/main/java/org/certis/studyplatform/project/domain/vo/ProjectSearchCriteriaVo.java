package org.certis.studyplatform.project.domain.vo;

import java.util.List;

/**
 * 프로젝트 검색 조건 VO
 *
 * CQRS Query 측면에서 사용되는 검색 조건
 * Repository 계층에서 동적 쿼리 구성에 활용
 */
public record ProjectSearchCriteriaVo(
        String keyword,
        String semester,
        String category,
        String subCategory,
        String status,
        List<String> skills
) {
    /**
     * 고급 검색용 팩토리 메소드
     *
     * @param keyword 검색 키워드
     * @param semester 학기
     * @param category 카테고리
     * @param subCategory 서브 카테고리
     * @param status 프로젝트 상태
     * @param skills 기술 스택
     * @return ProjectSearchCriteria
     */
    public static ProjectSearchCriteriaVo ofAdvanced(
            String keyword,
            String semester,
            String category,
            String subCategory,
            String status,
            List<String> skills
    ) {
        return new ProjectSearchCriteriaVo(
                keyword,
                semester,
                category,
                subCategory,
                status,
                skills
        );
    }

    /**
     * 기본 검색용 팩토리 메소드 (키워드만)
     *
     * @param keyword 검색 키워드
     * @return ProjectSearchCriteria
     */
    public static ProjectSearchCriteriaVo ofKeyword(String keyword) {
        return new ProjectSearchCriteriaVo(
                keyword,
                null, null, null, null, null
        );
    }

    /**
     * 빈 검색 조건 생성 (전체 조회용)
     *
     * @return ProjectSearchCriteria
     */
    public static ProjectSearchCriteriaVo empty() {
        return new ProjectSearchCriteriaVo(
                null, null, null, null, null, null
        );
    }
}