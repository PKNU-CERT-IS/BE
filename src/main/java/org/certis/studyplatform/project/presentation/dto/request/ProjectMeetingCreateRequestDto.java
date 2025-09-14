package org.certis.studyplatform.project.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Project Meeting Create Request DTO
 *
 * 프로젝트 회의록 생성 요청 데이터
 */
@Getter
@Setter
public class ProjectMeetingCreateRequestDto {

    @NotNull(message = "프로젝트 ID는 필수입니다")
    @Positive(message = "프로젝트 ID는 양수여야 합니다")
    private Long projectId;

    @NotBlank(message = "회의록 제목은 필수입니다")
    @Size(max = 255, message = "제목은 255자를 초과할 수 없습니다")
    private String title;

    @NotBlank(message = "회의록 내용은 필수입니다")
    @Size(max = 5000, message = "내용은 5000자를 초과할 수 없습니다")
    private String content;

    @NotNull(message = "참가자 목록은 필수입니다")
    private List<Long> participantIds;

    private String attachedUrl;
} 