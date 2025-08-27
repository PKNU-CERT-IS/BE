package org.certis.studyplatform.project.presentation.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Project Advanced Search Request DTO
 *
 * 프로젝트 고급 검색 요청 데이터
 * 5가지 필터 조건을 지원하는 복합 검색 DTO
 * @ModelAttribute로 사용되는 GET 파라미터 DTO
 */
@Getter
@Setter
public class ProjectAdvancedSearchRequestDto {

    /**
     * 키워드 검색 (title, description, creatorName 포함)
     */
    @Size(min = 1, max = 100, message = "검색 키워드는 1자 이상 100자 이하여야 합니다")
    private String keyword;

    /**
     * 학기 필터 (예: "2025-01", "2024-02")
     * 형식: YYYY-MM (01: 1학기, 02: 2학기)
     */
    @Pattern(regexp = "^\\d{4}-(01|02)$", message = "학기 형식이 올바르지 않습니다. 예: 2025-01, 2024-02")
    private String semester;

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
    @Pattern(regexp = "^(Ready|InProgress|Completed|READY|INPROGRESS|COMPLETED)$", 
             message = "상태는 Ready, InProgress, Completed 중 하나여야 합니다")
    private String status;

    /**
     * 모든 필터가 비어있는지 확인
     */
    public boolean isEmpty() {
        return (keyword == null || keyword.trim().isEmpty()) &&
               (semester == null || semester.trim().isEmpty()) &&
               (category == null || category.trim().isEmpty()) &&
               (subcategory == null || subcategory.trim().isEmpty()) &&
               (status == null || status.trim().isEmpty());
    }

    /**
     * 키워드가 설정되어 있는지 확인
     */
    public boolean hasKeyword() {
        return keyword != null && !keyword.trim().isEmpty();
    }

    /**
     * 학기 필터가 설정되어 있는지 확인
     */
    public boolean hasSemester() {
        return semester != null && !semester.trim().isEmpty();
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
        return status != null && !status.trim().isEmpty();
    }

    @Override
    public String toString() {
        return "ProjectAdvancedSearchRequestDto{" +
               "keyword='" + keyword + '\'' +
               ", semester='" + semester + '\'' +
               ", category='" + category + '\'' +
               ", subcategory='" + subcategory + '\'' +
               ", status='" + status + '\'' +
               '}';
    }
} 