package org.certis.studyplatform.member.application.object.command;

import java.time.OffsetDateTime;

public record UpdateGracePeriodCommand (Long memberId,
                                        OffsetDateTime gracePeriod) {
    public static UpdateGracePeriodCommand of(Long memberId, OffsetDateTime gracePeriod) {
        return new UpdateGracePeriodCommand(memberId, gracePeriod);
    }
}