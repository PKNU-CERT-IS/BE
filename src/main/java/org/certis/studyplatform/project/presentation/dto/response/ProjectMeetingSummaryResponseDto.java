package org.certis.studyplatform.project.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Project Meeting Summary Response DTO
 *
 * 프로젝트 회의록 요약 정보를 반환하는 DTO
 * Presentation Layer의 응답 DTO
 */
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMeetingSummaryResponseDto {

    private Long id;

    private String title;

    private String content;

    private Integer participantNumber;

    private String creatorName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime createdAt;

    private boolean isEditable;

    private java.util.List<ProjectMeetingSummaryResponseDto.Link> links;

    @Getter
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Link {
        private String title;
        private String url;
    }

    // toString for logging
    @Override
    public String toString() {
        return "ProjectMeetingSummaryResponseDto{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", participantNumber=" + participantNumber +
                ", creatorName='" + creatorName + '\'' +
                ", isEditable=" + isEditable +
                '}';
    }
}
