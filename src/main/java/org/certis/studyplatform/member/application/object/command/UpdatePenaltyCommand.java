package org.certis.studyplatform.member.application.object.command;

public record UpdatePenaltyCommand( Long memberId,
                                    Long penaltyPoints) {
    public static UpdatePenaltyCommand of(Long memberId, Long penaltyPoints) {
        return new UpdatePenaltyCommand(memberId, penaltyPoints);
    }
}