package org.certis.studyplatform.project.presentation.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.certis.studyplatform.project.domain.ProjectParticipantStatus;

import java.time.OffsetDateTime;

@Getter
@Builder(toBuilder = true)
public class ProjectParticipantDetailResponseDto {

    private Long id;
    private Long projectId;
    private String projectTitle;
    private Long memberId;
    private String memberName;
    private ProjectParticipantStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}