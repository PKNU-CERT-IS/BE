package org.certis.studyplatform.project.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.certis.studyplatform.shared.dto.LinkDto;

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

    private String title;

    private String content;

    @Positive(message = "참가자 수는 양수여야 합니다")
    private Integer participantNumber;

    private List<LinkDto> links;
} 