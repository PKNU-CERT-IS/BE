package org.certis.studyplatform.study.domain.vo;

import java.util.List;

/**
 * 스터디 검색 결과 VO
 *
 * CQRS Query 측면에서 사용되는 페이징된 검색 결과
 * Repository 계층에서 반환되는 검색 결과 래퍼
 */
public record StudySearchResultVo(
        List<StudySummaryVo> studies,
        Long totalElements,
        Integer totalPages,
        Integer currentPage,
        Integer pageSize
) {
    /**
     * 검색 결과 생성 팩토리 메소드
     *
     * @param studies 스터디 목록
     * @param totalElements 총 요소 수
     * @param totalPages 총 페이지 수
     * @param currentPage 현재 페이지
     * @param pageSize 페이지 크기
     * @return StudySearchResult
     */
    public static StudySearchResultVo of(
            List<StudySummaryVo> studies,
            Long totalElements,
            Integer totalPages,
            Integer currentPage,
            Integer pageSize
    ) {
        return new StudySearchResultVo(
                studies,
                totalElements,
                totalPages,
                currentPage,
                pageSize
        );
    }

    /**
     * 빈 검색 결과 생성
     *
     * @param pageSize 페이지 크기
     * @return StudySearchResult
     */
    public static StudySearchResultVo empty(Integer pageSize) {
        return new StudySearchResultVo(
                List.of(),
                0L,
                0,
                0,
                pageSize
        );
    }

    /**
     * 검색 결과가 비어있는지 확인
     *
     * @return 비어있으면 true
     */
    public boolean isEmpty() {
        return studies.isEmpty();
    }

    /**
     * 검색 결과가 있는지 확인
     *
     * @return 결과가 있으면 true
     */
    public boolean hasContent() {
        return !studies.isEmpty();
    }

    /**
     * 마지막 페이지인지 확인
     *
     * @return 마지막 페이지면 true
     */
    public boolean isLast() {
        return currentPage >= totalPages - 1;
    }

    /**
     * 첫 번째 페이지인지 확인
     *
     * @return 첫 번째 페이지면 true
     */
    public boolean isFirst() {
        return currentPage == 0;
    }
}