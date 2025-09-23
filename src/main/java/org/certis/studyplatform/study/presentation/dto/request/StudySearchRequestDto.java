package org.certis.studyplatform.study.presentation.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Study Search Request DTO
 *
 * 스터디 검색 요청 데이터
 */
@Getter
@Setter
public class StudySearchRequestDto {

    private String keyword;

    private String category;

    private String subCategory;

    private List<String> skills;

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
}