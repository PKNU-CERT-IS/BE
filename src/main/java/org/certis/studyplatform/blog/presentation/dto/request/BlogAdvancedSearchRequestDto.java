package org.certis.studyplatform.blog.presentation.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Blog Advanced Search Request DTO
 *
 * 블로그 고급 검색 요청 데이터
 * 5가지 필터 조건을 지원하는 복합 검색 DTO
 * @ModelAttribute로 사용되는 GET 파라미터 DTO
 */
@Getter
@Setter
public class BlogAdvancedSearchRequestDto {

    /**
     * 키워드 검색 (title, description, creatorName 포함)
     */
    @Size(min = 1, max = 100, message = "검색 키워드는 1자 이상 100자 이하여야 합니다")
    private String keyword;

    /**
     * 카테고리 필터
     */
    @Size(max = 50, message = "카테고리는 50자 이하여야 합니다")
    private String category;

    /**
     * 페이지 번호 (기본값 0)
     */
    @Min(0)
    private int page = 0;

    /**
     * 페이지 크기 (기본값 10)
     */
    @Min(1)
    private int size = 10;

    /**
     * 모든 필터가 비어있는지 확인
     */
    public boolean isEmpty() {
        return (keyword == null || keyword.trim().isEmpty()) &&
               (category == null || category.trim().isEmpty());
    }

    /**
     * 키워드가 설정되어 있는지 확인
     */
    public boolean hasKeyword() {
        return keyword != null && !keyword.trim().isEmpty();
    }


    /**
     * 카테고리 필터가 설정되어 있는지 확인
     */
    public boolean hasCategory() {
        return category != null && !category.trim().isEmpty();
    }


    @Override
    public String toString() {
        return "BlogAdvancedSearchRequestDto{" +
               "keyword='" + keyword + '\'' +
               ", category='" + category + '\'' +
               '}';
    }
} 