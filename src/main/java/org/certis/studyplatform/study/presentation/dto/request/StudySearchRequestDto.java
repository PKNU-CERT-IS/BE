package org.certis.studyplatform.study.presentation.dto.request;

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
}