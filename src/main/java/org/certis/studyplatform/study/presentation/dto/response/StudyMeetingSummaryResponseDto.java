package org.certis.studyplatform.study.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Study Meeting Summary Response DTO
 *
 * 스터디 회의록 요약 정보를 반환하는 DTO
 * Presentation Layer의 응답 DTO
 */
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class StudyMeetingSummaryResponseDto {

    private Long id;

    private String title;

    private Integer participantNumber;

    private String creatorName;

    private boolean isEditable;

    private String meetingAttachedUrl;

    private String meetingAttachedTitle;

    // toString for logging
    @Override
    public String toString() {
        return "StudyMeetingSummaryResponseDto{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", participantNumber=" + participantNumber +
                ", creatorName='" + creatorName + '\'' +
                ", isEditable=" + isEditable +
                '}';
    }
}
