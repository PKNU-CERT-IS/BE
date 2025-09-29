package org.certis.studyplatform.project.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.certis.studyplatform.shared.dto.LinkDto;

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
    private String title;

    @NotBlank(message = "회의록 내용은 필수입니다")
    private String content;

    @NotNull(message = "참가자 수는 필수입니다")
    @Positive(message = "참가자 수는 양수여야 합니다")
    private Integer participantNumber;

    private List<LinkDto> links;
} 