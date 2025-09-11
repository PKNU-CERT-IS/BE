package org.certis.studyplatform.study.presentation.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.certis.studyplatform.study.domain.vo.StudyStatus;

/**
 * Study Advanced Search Request DTO
 *
 * 스터디 고급 검색 요청 데이터
 * 5가지 필터 조건을 지원하는 복합 검색 DTO
 * @ModelAttribute로 사용되는 GET 파라미터 DTO
 */
@Getter
@Setter
public class StudyAdvancedSearchRequestDto {

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
     * 서브카테고리 필터
     */
    @Size(max = 50, message = "서브카테고리는 50자 이하여야 합니다")
    private String subcategory;

    /**
     * 상태 필터 (Ready, InProgress, Completed)
     */
    private StudyStatus status;

    /**
     * 모든 필터가 비어있는지 확인
     */
    public boolean isEmpty() {
        return (keyword == null || keyword.trim().isEmpty()) &&
               (category == null || category.trim().isEmpty()) &&
               (subcategory == null || subcategory.trim().isEmpty()) &&
               (status == null);
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

    /**
     * 서브카테고리 필터가 설정되어 있는지 확인
     */
    public boolean hasSubcategory() {
        return subcategory != null && !subcategory.trim().isEmpty();
    }

    /**
     * 상태 필터가 설정되어 있는지 확인
     */
    public boolean hasStatus() {
        return status != null;
    }

    @Override
    public String toString() {
        return "StudyAdvancedSearchRequestDto{" +
               "keyword='" + keyword + '\'' +
               ", category='" + category + '\'' +
               ", subcategory='" + subcategory + '\'' +
               ", status='" + status + '\'' +
               '}';
    }
} 