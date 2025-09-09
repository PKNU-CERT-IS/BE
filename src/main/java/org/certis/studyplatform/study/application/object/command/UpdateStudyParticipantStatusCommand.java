package org.certis.studyplatform.study.application.object.command;

import org.certis.studyplatform.study.domain.StudyParticipantStatus;

public record UpdateStudyParticipantStatusCommand(
        Long participantId,
        StudyParticipantStatus status,
        Long requesterId
) {
    public UpdateStudyParticipantStatusCommand {
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