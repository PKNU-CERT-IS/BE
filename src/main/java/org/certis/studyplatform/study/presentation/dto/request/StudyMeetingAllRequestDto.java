package org.certis.studyplatform.study.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

/**
 * Study Meeting All Request DTO
 *
 * 스터디 회의록 전체 목록 조회 요청 데이터
 */
@Getter
@Setter
public class StudyMeetingAllRequestDto {

    @NotNull(message = "스터디 ID는 필수입니다")
    @Positive(message = "스터디 ID는 양수여야 합니다")
    private Long studyId;

    // toString for logging
    @Override
    public String toString() {
        return "StudyMeetingAllRequestDto{" +
                "studyId=" + studyId +
                '}';
    }
} 