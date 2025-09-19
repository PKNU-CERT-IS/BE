package org.certis.studyplatform.project.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Project Meeting Update Request DTO
 *
 * 프로젝트 회의록 수정 요청 데이터
 */
@Getter
@Setter
public class ProjectMeetingUpdateRequestDto {

    @NotNull(message = "회의록 ID는 필수입니다")
    @Positive(message = "회의록 ID는 양수여야 합니다")
    private Long meetingId;

    @Size(max = 255, message = "제목은 255자를 초과할 수 없습니다")
    private String title;

    private String content;

    private String attachedUrl;

    private List<Long> participants;
} 