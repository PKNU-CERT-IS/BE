package org.certis.studyplatform.study.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Study Meeting Detail Response DTO
 *
 * 스터디 회의록 상세 정보를 반환하는 DTO
 * Presentation Layer의 응답 DTO
 */
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class StudyMeetingDetailResponseDto {

    private Long id;

    private Long studyId;

    private String title;

    private String content;

    private Integer participantNumber;

    private List<Long> participantIds;

    private Long writerId;

    private String writerName;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    private boolean isEditable;

    private List<StudyMeetingDetailResponseDto.Link> links;

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
        return "StudyMeetingDetailResponseDto{" +
                "id=" + id +
                ", studyId=" + studyId +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", writerId=" + writerId +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", isEditable=" + isEditable +
                '}';
    }
} 