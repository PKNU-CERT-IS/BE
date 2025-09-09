package org.certis.studyplatform.study.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Study Meeting Update Request DTO
 *
 * 스터디 회의록 수정 요청 데이터
 */
@Getter
@Setter
public class StudyMeetingUpdateRequestDto {

    @NotNull(message = "회의록 ID는 필수입니다")
    @Positive(message = "회의록 ID는 양수여야 합니다")
    private Long meetingId;

    @NotNull(message = "요청자 ID는 필수입니다")
    @Positive(message = "요청자 ID는 양수여야 합니다")
    private Long writerId;

    @Size(max = 255, message = "제목은 255자를 초과할 수 없습니다")
    private String title;

    @Size(max = 5000, message = "내용은 5000자를 초과할 수 없습니다")
    private String content;

    private String attachedUrl;

    private List<Long> participants;
} 