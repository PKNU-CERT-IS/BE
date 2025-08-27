package org.certis.studyplatform.project.application.object.command;

public record CancelProjectParticipantCommand(
        Long projectId,
        Long memberId
) {
    public CancelProjectParticipantCommand {
        if (projectId == null) {
            throw new IllegalArgumentException("Project ID cannot be null");
        }
        if (memberId == null) {
            throw new IllegalArgumentException("Member ID cannot be null");
        }
    }
}