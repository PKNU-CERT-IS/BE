package org.certis.studyplatform.project.presentation.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.certis.studyplatform.project.domain.ProjectParticipantStatus;

import java.time.OffsetDateTime;

@Getter
@Builder(toBuilder = true)
public class ProjectJoinResponseDto {

    private Long participantId;
    private Long projectId;
    private ProjectParticipantStatus status;
    private OffsetDateTime createdAt;
}
