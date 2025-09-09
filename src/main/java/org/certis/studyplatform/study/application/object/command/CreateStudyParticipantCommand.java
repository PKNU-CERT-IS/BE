package org.certis.studyplatform.study.application.object.command;

public record CreateStudyParticipantCommand(
        Long studyId,
        Long memberId
) {
    public CreateStudyParticipantCommand {
        if (studyId == null) {
            throw new IllegalArgumentException("Study ID cannot be null");
        }
        if (memberId == null) {
            throw new IllegalArgumentException("Member ID cannot be null");
        }
    }
}
