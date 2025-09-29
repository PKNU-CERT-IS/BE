package org.certis.studyplatform.study.presentation.dto.request;

import jakarta.validation.constraints.Min;
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
     * 학기 필터 (예: "2025-01", "2024-02")
     */
    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "학기는 YYYY-MM 형식이어야 합니다")
    private String semester;

    /**
     * 스터디 상태 필터 (Ready, InProgress, Completed)
     */
    private StudyStatus studyStatus;

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
               (category == null || category.trim().isEmpty()) &&
               (subcategory == null || subcategory.trim().isEmpty()) &&
               (semester == null || semester.trim().isEmpty()) &&
               (studyStatus == null);
    }

    /**
     * 키워드가 설정되어 있는지 확인
     */
    public boolean hasKeyword() {
        return keyword != null && !keyword.trim().isEmpty();
    }

    /**
     * 스터디 상태 필터가 설정되어 있는지 확인
     */
    public boolean hasStudyStatus() {
        return studyStatus != null;
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
     * 학기 필터가 설정되어 있는지 확인
     */
    public boolean hasSemester() {
        return semester != null && !semester.trim().isEmpty();
    }

    @Override
    public String toString() {
        return "StudyAdvancedSearchRequestDto{" +
               "keyword='" + keyword + '\'' +
               ", category='" + category + '\'' +
               ", subcategory='" + subcategory + '\'' +
               ", studyStatus='" + studyStatus + '\'' +
               '}';
    }
} 