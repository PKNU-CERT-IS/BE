package org.certis.studyplatform.member.application.object.command;

public record UpdatePenaltyCommand( Long memberId,
                                    Integer penaltyPoints) {
    public static UpdatePenaltyCommand of(Long memberId, Integer penaltyPoints) {
        return new UpdatePenaltyCommand(memberId, penaltyPoints);
    }
}