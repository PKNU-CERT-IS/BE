package org.certis.studyplatform.study.domain.vo;

/**
 * 스터디 검색 조건 VO
 *
 * CQRS Query 측면에서 사용되는 검색 조건
 * Repository 계층에서 동적 쿼리 구성에 활용
 */
public record StudySearchCriteriaVo(
        String keyword,
        String category,
        String subCategory,
        String semester,
        String status
) {
    /**
     * 고급 검색용 팩토리 메소드
     *
     * @param keyword 검색 키워드
     * @param category 카테고리
     * @param subCategory 서브 카테고리
     * @param semester 학기
     * @param status 스터디 상태
     * @return StudySearchCriteria
     */
    public static StudySearchCriteriaVo ofAdvanced(
            String keyword,
            String category,
            String subCategory,
            String semester,
            String status
    ) {
        return new StudySearchCriteriaVo(
                keyword,
                category,
                subCategory,
                semester,
                status
        );
    }

    /**
     * 기본 검색용 팩토리 메소드 (키워드만)
     *
     * @param keyword 검색 키워드
     * @return StudySearchCriteria
     */
    public static StudySearchCriteriaVo ofKeyword(String keyword) {
        return new StudySearchCriteriaVo(
                keyword,
                null, null, null, null
        );
    }

    /**
     * 빈 검색 조건 생성 (전체 조회용)
     *
     * @return StudySearchCriteria
     */
    public static StudySearchCriteriaVo empty() {
        return new StudySearchCriteriaVo(
                null, null, null, null, null
        );
    }
}