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
}


