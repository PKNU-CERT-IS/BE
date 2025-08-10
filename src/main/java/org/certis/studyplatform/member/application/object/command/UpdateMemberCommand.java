package org.certis.studyplatform.member.application.object.command;

import org.certis.studyplatform.member.domain.MemberRole;

import java.util.List;

public record UpdateMemberCommand(
        Long id,
        String name,
        String studentNumber,
        String grade,
        MemberRole role,
        String major,
        String description,
        List<String> skills
) {}