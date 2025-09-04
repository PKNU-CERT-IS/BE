package org.certis.studyplatform.member.application.object.command;

import org.certis.studyplatform.member.domain.MemberGrade;
import org.certis.studyplatform.member.domain.MemberRole;

public record UpdateMemberAdminFieldsCommand(
        Long executorId,
        MemberRole executorRole,
        Long targetMemberId,
        MemberRole newRole,
        MemberGrade newGrade
) {
    public static UpdateMemberAdminFieldsCommand of(
            Long executorId,
            MemberRole executorRole,
            Long targetMemberId,
            MemberRole newRole,
            MemberGrade newGrade) {
        return new UpdateMemberAdminFieldsCommand(
                executorId,
                executorRole,
                targetMemberId,
                newRole,
                newGrade
        );
    }
}