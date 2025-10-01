package org.certis.studyplatform.project.presentation.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.certis.studyplatform.project.domain.ProjectParticipantStatus;

import java.time.OffsetDateTime;

@Getter
@Builder(toBuilder = true)
public class ProjectParticipantStatusUpdateResponseDto {

    private Long projectId;
    private Long memberId;
    private ProjectParticipantStatus previousStatus;
    private ProjectParticipantStatus currentStatus;
    private String message;
    private OffsetDateTime updatedAt;
}