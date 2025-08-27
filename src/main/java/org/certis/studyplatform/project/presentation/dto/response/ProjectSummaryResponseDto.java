package org.certis.studyplatform.project.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Project Summary Response DTO
 *
 * 프로젝트 목록 조회 시 사용되는 요약 정보 응답 DTO
 * Presentation Layer의 응답 DTO
 */
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ProjectSummaryResponseDto {

    private Long id;

    private String title;

    private String description;

    private List<String> category;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime startDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime endDate;

    private String projectCreatorName;

    private String projectCreatorRole;

    private boolean isParticipantable;

    private String githubUrl;

    private String externalUrl;

    private String thumbnailUrl;

    // toString for logging
    @Override
    public String toString() {
        return "ProjectSummaryResponseDto{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", projectCreatorName='" + projectCreatorName + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                '}';
    }
}