package org.certis.studyplatform.project.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Project Update Request DTO
 *
 * 프로젝트 수정 요청 데이터
 */
@Getter
@Setter
public class ProjectUpdateRequestDto {

    @NotNull(message = "프로젝트 ID는 필수입니다")
    @Positive(message = "프로젝트 ID는 양수여야 합니다")
    private Long projectId;

    private String title;

    private String description;

    private String content;

    private String category;

    private String subCategory;

    private List<ProjectAttachedCreateRequestDto> attachments;

    private String githubUrl;

    private ExternalUrlRequestDto externalUrl;

    private String demoUrl;

    private String thumbnailUrl;

    @Positive(message = "최대 참여자 수는 양수여야 합니다")
    private Integer maxParticipants;


}