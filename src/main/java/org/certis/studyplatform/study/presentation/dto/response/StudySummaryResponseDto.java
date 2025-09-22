package org.certis.studyplatform.study.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.certis.studyplatform.member.domain.MemberGrade;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Study Summary Response DTO
 *
 * 스터디 목록 조회 시 사용되는 요약 정보 응답 DTO
 * Presentation Layer의 응답 DTO
 */
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class StudySummaryResponseDto {

    private Long id;

    private String title;

    private String description;

    private String category;

    private String subcategory;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime startDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime endDate;

    private String studyCreatorName;

    private MemberGrade studyCreatorGrade;

    private String semester;

    private String status;

    private boolean isParticipantable;

    private Integer currentParticipantNumber;

    private Integer maxParticipantNumber;

    private String thumbnailUrl;

    private List<StudyAttachedResponseDto> attachments;

    // toString for logging
    @Override
    public String toString() {
        return "StudySummaryResponseDto{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", studyCreatorName='" + studyCreatorName + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                '}';
    }
}