package org.certis.studyplatform.project.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Project Detail Response DTO
 *
 * 프로젝트 상세 조회 시 사용되는 상세 정보 응답 DTO
 * Presentation Layer의 응답 DTO
 */
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDetailResponseDto {

    private Long id;

    private String title;

    private String content;

    private String description;

    private String category;

    private String subCategory;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime startDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime endDate;

    private String creatorName;

    private String githubUrl;

    private String externalUrl;

    private String thumbnailUrl;

    private List<ProjectAttachedResponseDto> attachedFiles;

    private List<ProjectMeetingSummaryResponseDto> meetingSummaries;

    private List<ProjectParticipantSummaryResponseDto> participantSummaries;

    private Integer maxParticipants;

    private Integer currentParticipants;



    // toString for logging
    @Override
    public String toString() {
        return "ProjectDetailResponseDto{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", creatorName='" + creatorName + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                '}';
    }
} 