package org.certis.studyplatform.project.application.object.command;

import org.certis.studyplatform.project.domain.ProjectParticipantStatus;

public record UpdateProjectParticipantStatusCommand(
        Long participantId,
        ProjectParticipantStatus status,
        Long requesterId
) {
    public UpdateProjectParticipantStatusCommand {
        if (participantId == null) {
            throw new IllegalArgumentException("Participant ID cannot be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        if (requesterId == null) {
            throw new IllegalArgumentException("Requester ID cannot be null");
        }
    }
}