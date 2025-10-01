package org.certis.studyplatform.study.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Study Update Request DTO
 *
 * 스터디 수정 요청 데이터
 */
@Getter
@Setter
public class StudyUpdateRequestDto {

    @NotNull(message = "스터디 ID는 필수입니다")
    @Positive(message = "스터디 ID는 양수여야 합니다")
    private Long studyId;

    private String title;

    private String description;

    private String content;

    private String category;

    private String subCategory;

    private List<StudyAttachedCreateRequestDto> attachments;

    private String githubUrl;

    private String externalUrl;

    private String thumbnailUrl;

    @Positive(message = "최대 참여자 수는 양수여야 합니다")
    private Integer maxParticipants;
}