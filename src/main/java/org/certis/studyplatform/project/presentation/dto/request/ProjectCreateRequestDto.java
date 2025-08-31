package org.certis.studyplatform.project.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Project Create Request DTO
 *
 * 프로젝트 생성 요청 데이터
 */
@Getter
@Setter
public class ProjectCreateRequestDto {

    @NotBlank(message = "프로젝트 제목은 필수입니다")
    private String title;

    @NotBlank(message = "프로젝트 설명은 필수입니다")
    private String description;

    @NotNull(message = "내용은 필수입니다")
    private String content;

    @NotBlank(message = "카테고리는 필수입니다")
    private String category;

    @NotBlank(message = "서브 카테고리는 필수입니다")
    private String subCategory;

    @NotNull(message = "시작일은 필수입니다")
    private OffsetDateTime startDate;

    @NotNull(message = "종료일은 필수입니다")
    private OffsetDateTime endDate;

    private List<ProjectAttachedCreateRequestDto> attachedFiles;

    private String githubUrl;

    private String externalUrl;

    private String thumbnailUrl;

    @NotNull(message = "최대 참여자 수는 필수입니다")
    @Positive(message = "최대 참여자 수는 양수여야 합니다")
    private Integer maxParticipants;
}