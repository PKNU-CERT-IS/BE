package org.certis.studyplatform.project.presentation.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Project Search Request DTO
 *
 * 프로젝트 검색 요청 데이터
 */
@Getter
@Setter
public class ProjectSearchRequestDto {

    private String keyword;

    private String category;

    private String subCategory;

    private List<String> skills;
}