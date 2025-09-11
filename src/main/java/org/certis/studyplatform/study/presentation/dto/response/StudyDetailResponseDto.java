package org.certis.studyplatform.study.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.certis.studyplatform.member.domain.MemberGrade;
import org.certis.studyplatform.study.presentation.dto.response.StudyMeetingSummaryResponseDto;

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

    private String creatorName;

    private MemberGrade creatorGrade;

    private List<StudyAttachedResponseDto> attachedFiles;

    private List<StudyMeetingSummaryResponseDto> meetingSummaries;

    private List<StudyParticipantSummaryResponseDto> participantSummaries;

    private Integer maxParticipantNumber;

    private Integer currentParticipantNumber;



    // toString for logging
    @Override
    public String toString() {
        return "StudyDetailResponseDto{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", creatorName='" + creatorName + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                '}';
    }
} 