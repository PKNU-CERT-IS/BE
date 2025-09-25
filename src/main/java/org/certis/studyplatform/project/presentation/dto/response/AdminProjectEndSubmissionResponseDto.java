package org.certis.studyplatform.project.presentation.dto.response;

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
public class AdminProjectEndSubmissionResponseDto {

    private Long projectId;

    private ResultSubmitStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime submittedAt;

    private ProjectAttachedResponseDto attachment;

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


