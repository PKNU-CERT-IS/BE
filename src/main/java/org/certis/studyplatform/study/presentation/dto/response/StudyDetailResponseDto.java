package org.certis.studyplatform.study.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.certis.studyplatform.shared.domain.ResultSubmitStatus;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Study Detail Response DTO
 *
 * 프로젝트 상세 조회 시 사용되는 상세 정보 응답 DTO
 * Presentation Layer의 응답 DTO
 */
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class StudyDetailResponseDto {

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

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime updatedAt;

    private Long creatorId;

    private String studyCreatorName;

    private String studyCreatorGrade;

    private String studyCreatorProfileImageUrl;

    private String semester;

    private String status;

    private ResultSubmitStatus resultSubmitStatus;

    private String thumbnailUrl;

    private List<StudyAttachedResponseDto> attachments;

    private List<StudyMeetingSummaryResponseDto> meetingSummaries;

    private List<StudyParticipantSummaryResponseDto> participantSummaries;

    private Integer maxParticipantNumber;

    private Integer currentParticipantNumber;

    private boolean isParticipantable;



    // toString for logging
    @Override
    public String toString() {
        return "StudyDetailResponseDto{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", studyCreatorName='" + studyCreatorName + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                '}';
    }
} 