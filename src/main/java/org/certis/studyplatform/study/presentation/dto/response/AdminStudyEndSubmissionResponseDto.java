package org.certis.studyplatform.study.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.certis.studyplatform.shared.domain.ResultSubmitStatus;

import java.time.OffsetDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStudyEndSubmissionResponseDto {

    private Long studyId;

    private ResultSubmitStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime submittedAt;

    private StudyAttachedResponseDto attachment;

    // additional fields for admin view
    private String category;
    private String subCategory;
    private String title;
    private String description;
    private Long creatorId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private java.time.OffsetDateTime startedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private java.time.OffsetDateTime endedAt;

    private Integer currentParticipantNumber;
    private Integer maxParticipantNumber;
}


